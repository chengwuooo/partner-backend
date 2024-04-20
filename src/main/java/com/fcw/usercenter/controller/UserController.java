package com.fcw.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fcw.usercenter.common.BaseResponse;
import com.fcw.usercenter.common.ErrorCode;
import com.fcw.usercenter.common.ResultUtils;
import com.fcw.usercenter.exception.BusinessException;
import com.fcw.usercenter.model.domain.User;
import com.fcw.usercenter.model.domain.request.UserLoginRequest;
import com.fcw.usercenter.model.domain.request.UserRegisterRequest;
import com.fcw.usercenter.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static com.fcw.usercenter.contant.UserConstant.ADMIN_ROLE;
import static com.fcw.usercenter.contant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户接口
 *
 * @author fcw
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    @RequestMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();

        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数不能为空");
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return new BaseResponse<>(0, result, "注册成功");
    }

    @RequestMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();

        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数不能为空");
        }
        User result = userService.userLogin(userAccount, userPassword, request);
        return new BaseResponse<>(0, result, "登录成功");
    }

    @GetMapping("/current")
    public BaseResponse<User> getCurrent(HttpServletRequest request) {
        User userObj = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null)
            throw new BusinessException(ErrorCode.NULL_LOGIN);

        Long userId = currentUser.getId();
        // TODO 校验用户是否合法
        User user = userService.getById(userId);

        User safeUser = userService.getSafeUser(user);
        return ResultUtils.success(safeUser);
    }

    @GetMapping("search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        //仅管理员可以查看用户列表
        if (!isAdmin(request))
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限");

        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            userQueryWrapper.like("username", username);
        }
        List<User> userList = userService.list(userQueryWrapper);
        List<User> list = userList.stream().map(user -> {
            return userService.getSafeUser(user);
        }).collect(Collectors.toList());
        return ResultUtils.success(list);
    }


    @PostMapping("delete")
    public BaseResponse<Boolean> deleteUser(@RequestParam Long id, HttpServletRequest request) {
        //仅管理员可以删除用户
        if (!isAdmin(request))
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限");

        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数错误");
        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }

    @PostMapping("logout")
    public BaseResponse<String> userLogout(HttpServletRequest request) {
        if (request == null)
            return null;
        userService.userLogout(request);
        return ResultUtils.success("登出成功");
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    private boolean isAdmin(HttpServletRequest request) {
        User userObj = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null || userObj.getUserRole() != ADMIN_ROLE)
            return false;
        return true;
    }


}
