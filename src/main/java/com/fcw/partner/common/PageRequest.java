package com.fcw.partner.common;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用请求的分页
 */

@Data

public class PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -3981532167500043307L;
    /**
     * 页面大小
     */
    protected int pageSize = 10;

    /**
     * 第几页
     */
    protected int pageNum = 1;
}
