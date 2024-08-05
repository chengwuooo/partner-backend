package com.fcw.partner.controller;

import com.fcw.partner.common.BaseResponse;
import com.fcw.partner.common.ErrorCode;
import com.fcw.partner.common.ResultUtils;
import com.fcw.partner.exception.BusinessException;
import com.fcw.partner.model.domain.User;
import com.fcw.partner.service.FollowsService;
import com.fcw.partner.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Chengwu Fang
 * date 2024/7/11
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private UserService userService;

    @Resource
    private FollowsService followsService;

    @PostMapping("follow")
    public BaseResponse<Boolean> followUser(@RequestParam("followId") Long followId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null)
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        if (followId == null || followId <= 0)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        boolean b = followsService.followUser(loginUser.getId(), followId);
        return ResultUtils.success(b);
    }

    @PostMapping("unfollow")
    public BaseResponse<Boolean> unfollowUser(@RequestParam Long followId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null)
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        if (followId == null || followId <= 0)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        boolean b = followsService.unfollowUser(loginUser.getId(), followId);
        return ResultUtils.success(b);
    }

    @GetMapping("isFollow")
    public BaseResponse<Boolean> isFollow(@RequestParam Long followId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null)
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        if (followId == null || followId <= 0)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        boolean b = followsService.isFollow(loginUser.getId(), followId);
        return ResultUtils.success(b);
    }

    @GetMapping("list")
    public BaseResponse<List<User>> listFollows(@RequestParam("type") String listType, HttpServletRequest request) {
        List<User> userList;
        User loginUser = userService.getLoginUser(request);
        if (listType.equals("myFollows")){
            userList = followsService.listFollows(loginUser);
        } else if (listType.equals("mutual")) {
            userList = followsService.listMutualFollows(loginUser);

        } else if (listType.equals("fans")) {
            userList = followsService.listFans(loginUser);
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        return ResultUtils.success(new ArrayList<>(userList));
    }






}
