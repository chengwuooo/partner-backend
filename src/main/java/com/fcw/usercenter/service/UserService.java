package com.fcw.usercenter.service;

import com.fcw.usercenter.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

/**
* @author chengwu
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2024-03-20 16:58:34
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @param checkPassword 确认密码
     * @return 用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword );

    /**
     * 用户登录
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword,HttpServletRequest request);

    /**
     * 用户脱敏
     * @param user
     * @return
     */
    User getSafeUser(User user);

    /**
     * 用户登出
     *
     * @return
     */
    void userLogout(HttpServletRequest request);
}
