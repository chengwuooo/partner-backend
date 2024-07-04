package com.fcw.partner.model.request;

import lombok.Data;
import java.io.Serializable;

/**
 * @author Chengwu Fang
 * 2024/7/3
 */
@Data
public class MessageRequest implements Serializable {
    private static final long serialVersionUID = 394238155496276082L;
    private Long fromId;
    private Long toId;
    private String text;
    private Integer messageType;
}
