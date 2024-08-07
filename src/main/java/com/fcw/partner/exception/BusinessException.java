package com.fcw.partner.exception;

import com.fcw.partner.common.ErrorCode;

/**
 * 自定义异常类
 */
public class BusinessException  extends RuntimeException{
    private final int code;
    private final String description;


    public BusinessException(ErrorCode errorCode, String description) {
        super(errorCode.getChat());
        this.code = errorCode.getCode();
        this.description = description;
    }
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getChat());
        this.code = errorCode.getCode();
        this.description = "";
    }

    public int getCode() {
        return code;
    }
    public String getDescription() {
        return description;
    }
}
