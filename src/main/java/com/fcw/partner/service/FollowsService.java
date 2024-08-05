package com.fcw.partner.service;

import com.fcw.partner.model.domain.Follows;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fcw.partner.model.domain.User;

import java.util.List;

/**
* @author chengwu
* @description 针对表【follows(关注表)】的数据库操作Service
* @createDate 2024-07-10 19:29:39
*/
public interface FollowsService extends IService<Follows> {

    /**
     * 关注用户
     * @param userId 用户id
     * @param followId 被关注用户id
     * @return
     */
    boolean followUser(Long userId, Long followId);

    boolean unfollowUser(Long userId, Long followId);

    boolean isFollow(Long id, Long followId);

    /**
     * 获取关注列表
     * @param loginUser 登录用户
     * @return
     */
    List<User> listFollows(User loginUser);

    /**
     * 获取互相关注列表
     * @param loginUser 登录用户
     * @return
     */
    List<User> listMutualFollows(User loginUser);

    /**
     *
     * @param loginUser 登录用户
     */
    List<User> listFans(User loginUser);
}
