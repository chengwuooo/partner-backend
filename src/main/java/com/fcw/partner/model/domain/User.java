package com.fcw.partner.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户
 * &#064;TableName  user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 头像
     */
    private String userAvatar;

    /**
     * 
     */
    private Integer gender;
    /**
     * 个人简介
     */
    private String userProfile;
    /**
     * 
     */
    private String userPassword;

    /**
     * 
     */
    private String phone;

    /**
     * 
     */
    private String email;

    /**
     * 用户状态
     */
    private Integer userStatus;

    /**
     * 
     */
    private Date createTime;

    /**
     * 更新日期
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;


    private String userRole;

    /**
     * 标签列表
     */
    private String tags;


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}