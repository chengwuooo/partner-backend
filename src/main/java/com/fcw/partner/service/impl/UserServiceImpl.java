package com.fcw.partner.service.impl;

import java.nio.charset.StandardCharsets;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fcw.partner.common.ErrorCode;
import com.fcw.partner.exception.BusinessException;
import com.fcw.partner.model.domain.User;
import com.fcw.partner.service.UserService;
import com.fcw.partner.mapper.UserMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.fcw.partner.contant.UserConstant.USER_LOGIN_STATE;

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
    public long userRegister(String userAccount,String userPassword,String checkPassword) {
        //1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword )) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数不能为空");
        }
        if (userAccount.length() < 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号不能少于3位");
        }
        if (userPassword.length() < 7 || checkPassword.length() < 7) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能少于7位");
        }

        //账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号不能包含特殊字符");
        }

        //密码和确认密码必须一致
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码和确认密码不一致");
        }

        //2.对密码进行加密
        String newPassword = DigestUtils.md5DigestAsHex((userPassword + salt).getBytes(StandardCharsets.UTF_8));

        //账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
//        long count = this.count(queryWrapper);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号已存在");
        }



        //3.保存到数据库
        User user = new User();
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
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号不能包含特殊字符");
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
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名或密码错误");
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
        safetyUser.setUsername(user.getUsername());
        safetyUser.setAvatarUrl(user.getAvatarUrl());
        safetyUser.setGender(user.getGender());
        safetyUser.setUserRole(user.getUserRole());
        safetyUser.setPhone(user.getPhone());
        safetyUser.setEmail(user.getEmail());
        safetyUser.setUserStatus(user.getUserStatus());
        safetyUser.setCreateTime(user.getCreateTime());
        safetyUser.setTags(user.getTags());

        return safetyUser;
    }

    @Override
    public void userLogout(HttpServletRequest request) {
        request.getSession().removeAttribute(USER_LOGIN_STATE);
    }

    /**
     * 根据标签搜索用户(内存过滤）
     * @param tagNameList 标签名称列表，用于搜索用户的标签依据。这个参数不能为空，否则会抛出业务异常。
     * @return 返回符合所有标签条件的用户列表。返回的用户列表已经做了安全处理。
     * @throws BusinessException 如果标签列表为空，则抛出业务异常，提示参数错误。
     */
    @Override
    public List<User> searchUserByTags(List<String> tagNameList){
        // 校验输入的标签列表是否为空，如果为空，则抛出业务异常
        if (tagNameList.size()==0)
            throw new BusinessException(ErrorCode.PARAM_ERROR);

        // 创建查询包装器
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);

        // 使用Gson对用户标签进行处理，以判断用户是否符合搜索条件
        Gson gson = new Gson();
        userList.stream().filter(user -> {
            String tagstr = user.getTags();
            if (StringUtils.isBlank(tagstr)){
                return false;
            }
            Set<String> tempTagNameSet =  gson.fromJson(tagstr,new TypeToken<Set<String>>(){}.getType());
            tempTagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());

            for (String tagName : tagNameList){
                if (!tempTagNameSet.contains(tagName)){
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());

        // 返回经过安全处理的用户列表
        return  userList;
    }

    /**
     * 根据标签搜索用户。
     * 该方法将根据提供的用户名关键字搜索用户，返回与关键字匹配的用户列表。
     * @param username 要搜索的用户名关键字。
     * @return 返回一个包含与搜索关键字匹配的用户列表的集合。
     */
    @Override
    public List<User> searchUsersByUsername(String username){

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 创建查询包装器，用于构建SQL查询条件

            queryWrapper = queryWrapper.like("username", username);

        // 根据查询条件查询用户列表
        List<User> userList = userMapper.selectList(queryWrapper);

        // 对查询结果进行处理，返回安全用户信息列表
        return  userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }




}




