package com.fcw.usercenter.common;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 错误码
 */
@AllArgsConstructor

public enum ErrorCode {
    SUCCESS(0, "成功"),
    PARAM_ERROR(40000, "请求参数错误"),
    NULL_ERROR(40001, "请求数据为空"),
    NULL_LOGIN(40100, "未登录"),
    NO_AUTH(40101, "无权限"),
    SYSTEM_ERROR(50000, "系统内部异常"),


    ;
    private final int code;
    private final String message;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
