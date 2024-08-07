package com.fcw.partner.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Chengwu Fang
 * date 2021-07-03
 */
@Data
public class MessageVo implements Serializable {
    private static final long serialVersionUID = -336328171592225010L;
    private WebSocketVo fromUser;
    private WebSocketVo toUser;
    private Long teamId;
    private String text;
    private Integer chatType;
    private Boolean isMy = false;
    private Boolean isAdmin = false;
    private String createTime;
}
