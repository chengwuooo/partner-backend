package com.fcw.usercenter.common;

/**
 * 通用工具类
 */
public class ResultUtils {
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    public static <T> BaseResponse<T> success(int code, T data) {
        return new BaseResponse<>(code, data);
    }

    public static <T> BaseResponse<T> success(int code, T data, String message) {
        return new BaseResponse<>(code, data, message);
    }


    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    public static <T> BaseResponse<T> error(int code, String message, String description) {
        return new BaseResponse<>(code, message, description);
    }

    public static <T> BaseResponse<T> error(ErrorCode errorCode ,String message, String description) {
        return new BaseResponse<>(errorCode,message,description);
    }

    public static <T> BaseResponse<T> error(ErrorCode errorCode , String description) {
        return new BaseResponse<>(errorCode,description);
    }



}
