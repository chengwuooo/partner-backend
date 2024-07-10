package com.fcw.partner.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fcw.partner.common.ErrorCode;
import com.fcw.partner.exception.BusinessException;
import com.fcw.partner.mapper.UserMapper;
import com.fcw.partner.model.domain.User;
import com.fcw.partner.service.UserService;
import com.fcw.partner.utils.AlgorithmUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.fcw.partner.constant.UserConstant.ADMIN_ROLE;
import static com.fcw.partner.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author chengwu
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2024-03-20 16:58:34
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    @Resource
    private UserMapper userMapper;
    /**
     * 加密盐
     */
    private static final String salt = "fcw";

    @Override
    public long userRegister(String userName,String userAccount, String userPassword, String checkPassword) {
        //1.校验
        if (StringUtils.isAnyBlank(userName,userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        if (userAccount.length() < 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能少于3位");
        }
        if (userPassword.length() < 7 || checkPassword.length() < 7) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能少于7位");
        }

        //账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }

        //密码和确认密码必须一致
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码和确认密码不一致");
        }

        //2.对密码进行加密
        String newPassword = DigestUtils.md5DigestAsHex((userPassword + salt).getBytes(StandardCharsets.UTF_8));

        //账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
//        long count = this.count(queryWrapper);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }


        //3.保存到数据库
        User user = new User();
        user.setUserName(userName);
        user.setUserAccount(userAccount);
        user.setUserPassword(newPassword);
//        boolean saveResult = this.save(user);
        int saveResult = userMapper.insert(user);
        if (saveResult < 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }
        return user.getId();
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        if (userAccount.length() < 3) {
            return null;
        }
        if (userPassword.length() < 7) {
            return null;
        }
        //账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }
        //2.加密
        String newPassword = DigestUtils.md5DigestAsHex((userPassword + salt).getBytes(StandardCharsets.UTF_8));

        //3.查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", newPassword);
        User user = userMapper.selectOne(queryWrapper);
        //用户不存在
        if (user == null) {
            log.info("用户登录失败，用户名或密码错误");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名或密码错误");
        }

        //4.脱敏用户信息
        User safetyUser = getSafetyUser(user);

        //5.记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);

        return safetyUser;
    }

    /**
     * 用户脱敏
     *
     * @param user
     * @return
     */
    @Override
    public User getSafetyUser(User user) {
        if (user == null)
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户不存在");
        User safetyUser = new User();
        safetyUser.setId(user.getId());
        safetyUser.setUserAccount(user.getUserAccount());
        safetyUser.setUserName(user.getUserName());
        safetyUser.setUserAvatar(user.getUserAvatar());
        safetyUser.setGender(user.getGender());
        safetyUser.setUserRole(user.getUserRole());
        safetyUser.setPhone(user.getPhone());
        safetyUser.setEmail(user.getEmail());
        safetyUser.setUserStatus(user.getUserStatus());
        safetyUser.setCreateTime(user.getCreateTime());
        safetyUser.setTags(user.getTags());
        safetyUser.setUserProfile(user.getUserProfile());

        return safetyUser;
    }

    @Override
    public void userLogout(HttpServletRequest request) {
        request.getSession().removeAttribute(USER_LOGIN_STATE);
    }

    /**
     * 根据标签搜索用户。
     *
     * @param tagNameList 用户要搜索的标签
     * @return
     */
    @Override
    public List<User> searchUserByTags(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //拼接tag
        // like '%Java%' and like '%Python%'
        for (String tagList : tagNameList) {
            queryWrapper = queryWrapper.like("tags", tagList);
        }
        List<User> userList = userMapper.selectList(queryWrapper);
        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());

//        //1.先查询所有用户
//        QueryWrapper queryWrapper = new QueryWrapper<>();
//        List<User> userList = userMapper.selectList(queryWrapper);
//        Gson gson = new Gson();
//        //2.判断内存中是否包含要求的标签
//        return userList.stream().filter(user -> {
//            String tagstr = user.getTags();
//
//
//
//            if (StringUtils.isBlank(tagstr)){
//                return false;
//            }
//            Set<String> tempTagNameSet =  gson.fromJson(tagstr,new TypeToken<Set<String>>(){}.getType());
//            System.out.println("tagstr:"+tagstr);
//            for (String tagName : tagNameList){
//                if (!tempTagNameSet.contains(tagName)){
//                    return false;
//                }
//            }
//            System.out.println("::::::::::::::::::::::::::::::::::::::::::::::::::::::::::");
//            return true;
//        }).map(this::getSafetyUser).collect(Collectors.toList());
    }

//    /**
//     * 根据标签搜索用户(内存过滤）
//     * @param tagNameList 标签名称列表，用于搜索用户的标签依据。这个参数不能为空，否则会抛出业务异常。
//     * @return 返回符合所有标签条件的用户列表。返回的用户列表已经做了安全处理。
//     * @throws BusinessException 如果标签列表为空，则抛出业务异常，提示参数错误。
//     */
//    @Override
//    public List<User> searchUserByTags(List<String> tagNameList){
//        // 校验输入的标签列表是否为空，如果为空，则抛出业务异常
//        if (tagNameList.size()==0)
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//
//        // 创建查询包装器
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        List<User> userList = userMapper.selectList(queryWrapper);
//
//        // 使用Gson对用户标签进行处理，以判断用户是否符合搜索条件
//        Gson gson = new Gson();
//        userList.stream().filter(user -> {
//            String tagstr = user.getTags();
//            if (StringUtils.isBlank(tagstr)){
//                return false;
//            }
//            Set<String> tempTagNameSet =  gson.fromJson(tagstr,new TypeToken<Set<String>>(){}.getType());
//            tempTagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
//
//            for (String tagName : tagNameList){
//                if (!tempTagNameSet.contains(tagName)){
//                    return false;
//                }
//            }
//            return true;
//        }).map(this::getSafetyUser).collect(Collectors.toList());
//
//        // 返回经过安全处理的用户列表
//        return  userList;
//    }

    /**
     * 根据标签搜索用户。
     * 该方法将根据提供的用户名关键字搜索用户，返回与关键字匹配的用户列表。
     *
     * @param userName 要搜索的用户名关键字。
     * @return 返回一个包含与搜索关键字匹配的用户列表的集合。
     */
    @Override
    public List<User> searchUsersByUserName(String userName) {

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 创建查询包装器，用于构建SQL查询条件

        queryWrapper = queryWrapper.like("userName", userName);

        // 根据查询条件查询用户列表
        List<User> userList = userMapper.selectList(queryWrapper);

        // 对查询结果进行处理，返回安全用户信息列表
        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }

    @Override
    public int updateUser(User updateUser, User loginUser) throws BusinessException {
        // 检查 loginUser 是否为 null
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long updateId = updateUser.getId();
        // 检查 updateId 是否合法
        if (updateId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 管理员可修改任何用户的信息
        if (isAdmin(loginUser)) {
            User oldUser = userMapper.selectById(updateId);
            // 当尝试更新的用户不存在时抛出异常
            if (oldUser == null) {
                throw new BusinessException(ErrorCode.NULL_ERROR);
            }
            userMapper.updateById(updateUser);
        }
        // 用户只能修改自己的信息
        else {
            // 若不是管理员且尝试更新的用户ID不是自己的，则无权限修改
            if (updateId != loginUser.getId()) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            userMapper.updateById(updateUser);
        }
        // 返回更新成功的记录数（此处根据实际需求调整，假设updateById返回了更新的记录数）
        return 1;
    }


    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null)
            return null;
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null)
            throw new BusinessException(ErrorCode.NOT_LOGIN);

        return (User) userObj;
    }

    @Override
    public boolean isAdmin(User loginUser) {
        if (loginUser == null || loginUser.getUserRole() != ADMIN_ROLE)
            return false;
        return true;
    }

    @Override
    public boolean isAdmin(HttpServletRequest request) {
        User userObj = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null || userObj.getUserRole() != ADMIN_ROLE)
            return false;
        return true;
    }

    /**
     * 根据登录用户的标签匹配其他用户。
     *
     * @param num
     * @param loginUser 当前登录的用户，将根据此用户的标签进行匹配。
     * @return 匹配到的用户列表，按标签相似度排序，相似度高者优先。
     */
    @Override
    public List<User> matchUsers(long num, User loginUser) {
        // 构建查询条件，只查询标签不为空的用户，并只选择id和tags字段
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "tags");
        queryWrapper.isNotNull("tags");
        List<User> userList = this.list(queryWrapper);

        // 获取登录用户的标签，并将其转换为列表形式
        String tags = loginUser.getTags();
        System.out.println("tags:"+tags);
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());

        // 计算当前用户与所有其他用户的标签相似度，并保存用户及其相似度
        List<Pair<User, Long>> list = new ArrayList<>();
        for (User user : userList) {
            String userTags = user.getTags();
            // 跳过没有标签或为当前用户的记录
            if (StringUtils.isBlank(userTags) || Objects.equals(user.getId(), loginUser.getId())) {
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
//            System.out.println("tagList"+tagList+"userTagList:"+userTagList);
            // 计算标签编辑距离，作为相似度
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            list.add(new Pair<>(user, distance));
        }

        // 根据相似度对用户进行排序，并选取前num个
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());

        // 获取排序后的用户ID列表，用于后续查询完整的用户信息
        List<Long> userIdList = topUserPairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());

        // 根据用户ID列表查询用户信息
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", userIdList);
        Map<Long, List<User>> userIdUserListMap = this.list(userQueryWrapper)
                .stream()
                .map(this::getSafetyUser)
                .collect(Collectors.groupingBy(User::getId));

        // 根据用户ID获取最终的用户对象列表，并返回
        List<User> finalUserList = new ArrayList<>();
        for (Long userId : userIdList) {
            finalUserList.add(userIdUserListMap.get(userId).get(0));
        }
        return finalUserList;
    }
}




