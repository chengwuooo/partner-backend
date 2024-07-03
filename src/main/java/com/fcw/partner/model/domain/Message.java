package com.fcw.partner.model.domain;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.util.Date;

/**
 * @author Chengwu Fang
 * @createDate 2024-07-03
 * @description 聊天消息类，用于表示聊天应用中的消息实体。
 */
@Data
public class Message {
    /**
     * 消息主键ID，由数据库自动生成。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息发送者的用户ID。
     */
    private Long fromId;

    /**
     * 消息接收者的用户ID。
     */
    private Long toId;

    /**
     * 消息的内容。
     */
    private String text;

    /**
     * 消息的类型，用于区分不同类型的消息，如文本、图片等。
     */
    private Integer type;

    /**
     * 消息的发送时间，格式化为"yyyy-MM-dd HH:mm:ss"。
     */
    @JSONField(format="yyyy-MM-dd HH:mm:ss")
    private Date date;

    /**
     * 消息的阅读状态，标识消息是否已被接收者阅读。
     */
    private Integer isRead;

    /**
     * 消息的删除状态，用于逻辑删除，标记为已删除的消息在查询时将被过滤。
     */
    @TableLogic
    private Integer isDelete;
}