package com.fcw.partner.model.vo;

import java.io.Serializable;

/**
 * @author Chengwu Fang
 * date 2021-07-03
 */
public class WebSocketVo implements Serializable {
    private static final long serialVersionUID = -6619216013263024984L;
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
}
