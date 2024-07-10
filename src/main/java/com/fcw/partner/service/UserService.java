package com.fcw.partner.service;

import com.fcw.partner.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


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
    long userRegister(String userName,String userAccount, String userPassword, String checkPassword );

    /**
     * 用户登录
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     * @param user
     * @return
     */
    User getSafetyUser(User user);

    /**
     * 用户登出
     *
     * @return
     */
    void userLogout(HttpServletRequest request);

    /**
     * 根据标签名列表搜索用户
     * @param tagNameList 标签名列表
     * @return
     */
    List<User> searchUserByTags(List<String> tagNameList);

    List<User> searchUsersByUserName(String userName);

    /**
     * @param updateUser 更新的用户信息
     * @param loginUser 登录用户
     * @return
     */
    int updateUser(User updateUser, User loginUser);

    /**
     * 获取登录用户
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 判断是否是管理员
     * @return
     */
    boolean isAdmin(HttpServletRequest request) ;

    /**
     * 判断是否是管理员
     * @param loginUser 登录用户
     */
    boolean isAdmin(User loginUser);

    /**
     * 匹配用户
     */
    List<User> matchUsers(long num, User loginUser);
}
