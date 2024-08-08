package com.fcw.partner.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fcw.partner.common.ErrorCode;
import com.fcw.partner.exception.BusinessException;
import com.fcw.partner.mapper.UserMapper;
import com.fcw.partner.model.domain.Follows;
import com.fcw.partner.model.domain.User;
import com.fcw.partner.service.FollowsService;
import com.fcw.partner.mapper.FollowsMapper;
import com.fcw.partner.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.fcw.partner.constant.FollowConstant.IS_ACTIVE;

/**
 * @author chengwu
 * @description 针对表【follows(关注表)】的数据库操作Service实现
 * @createDate 2024-07-10 19:29:39
 */
@Service
public class FollowsServiceImpl extends ServiceImpl<FollowsMapper, Follows>
        implements FollowsService {
    @Resource
    private UserMapper userMapper;

    @Resource
    private FollowsMapper followsMapper;

    @Resource
    private UserService userService;

    @Override
    public boolean followUser(Long userId, Long followId) {
        //1.校验
        if (userId == null || followId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //2.查询用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户不存在");
        }
        User followUser = userMapper.selectById(followId);
        if (followUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "被关注用户不存在");
        }

        //3.查询是否已经关注
        LambdaQueryWrapper<Follows> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Follows::getUser_id, userId);
        lambdaQueryWrapper.eq(Follows::getFollowed_id, followId);
        Follows follows = followsMapper.selectOne(lambdaQueryWrapper);
        //查询是否曾经关注过
        if (follows != null) {
            //已经关注过或者被取消关注
            if (follows.getIs_active() == 1)
                throw new BusinessException(ErrorCode.NULL_ERROR, "已经关注过");
            else {
                follows.setIs_active(1);
                follows.setCreated_at(new Date());
                followsMapper.updateById(follows);
                return true;
            }
        }
        //4.关注
        follows = new Follows();
        follows.setUser_id(userId);
        follows.setFollowed_id(followId);
        follows.setIs_active(1);
        follows.setCreated_at(new Date());
        followsMapper.insert(follows);
        return true;
    }

    @Override
    public boolean unfollowUser(Long userId, Long followId) {
        //1.校验
        if (userId == null || followId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //2.查询用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户不存在");
        }
        User followUser = userMapper.selectById(followId);
        if (followUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "被关注用户不存在");
        }

        //3.查询是否已经关注
        LambdaQueryWrapper<Follows> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Follows::getUser_id, userId);
        lambdaQueryWrapper.eq(Follows::getFollowed_id, followId);
        Follows follows = followsMapper.selectOne(lambdaQueryWrapper);
        if (follows == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "未关注过");
        }
        //4.取消关注
        follows.setIs_active(0);
        follows.setCreated_at(new Date());
        followsMapper.updateById(follows);
        return true;
    }

    @Override
    public boolean isFollow(Long id, Long followId) {
        //1.校验
        if (id == null || followId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2.查询用户是否存在
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户不存在");
        }
        User followUser = userMapper.selectById(followId);
        if (followUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "被关注用户不存在");
        }
        //3.查询是否已经关注
        LambdaQueryWrapper<Follows> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Follows::getUser_id, id);
        lambdaQueryWrapper.eq(Follows::getFollowed_id, followId);
        Follows follows = followsMapper.selectOne(lambdaQueryWrapper);
        if (follows == null) {
            return false;
        }
        return follows.getIs_active() == 1;
    }

    /**
     * 获取关注列表
     *
     * @param loginUser 登录用户
     * @return
     */
    @Override
    public List<User> listFollows(User loginUser) {
        long loginUserID = loginUser.getId();

        List<User> userList = followsMapper.listFollows(loginUserID, true);

        return userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
    }

    /**
     * 获取互相关注列表
     *
     * @param loginUser 登录用户
     * @return
     */
    @Override
    public List<User> listMutualFollows(User loginUser) {
        long loginUserId = loginUser.getId();

        List<User> userList = followsMapper.listMutualFollowUsers(loginUserId, true);

        return userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
    }

    /**
     * @param loginUser 登录用户
     */
    @Override
    public List<User> listFans(User loginUser) {
        long loginUserId = loginUser.getId();

        // 1. 查询关注登录用户的用户ID列表
        LambdaQueryWrapper<Follows> listFansQueryWrapper = new LambdaQueryWrapper<>();
        listFansQueryWrapper.eq(Follows::getFollowed_id, loginUserId)
                .eq(Follows::getIs_active, IS_ACTIVE);
        List<Follows> loginUserFansList = followsMapper.selectList(listFansQueryWrapper);
        List<Long> loginUserFanIds = loginUserFansList.stream()
                .map(Follows::getUser_id)
                .collect(Collectors.toList());

        // 2. 根据用户ID列表查询用户信息
        return userService.listUsersByIds(new ArrayList<>(loginUserFanIds));
    }

}




