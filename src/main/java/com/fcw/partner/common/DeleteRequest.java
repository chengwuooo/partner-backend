package com.fcw.partner.common;

import lombok.Data;


import java.io.Serializable;

/**
 * 通用删除请求
 *
 */
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = -1208581002509309215L;

    private long id;
}
