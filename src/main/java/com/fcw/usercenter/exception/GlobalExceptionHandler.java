package com.fcw.usercenter.exception;

import com.fcw.usercenter.common.BaseResponse;
import com.fcw.usercenter.common.ErrorCode;
import com.fcw.usercenter.common.ResultUtils;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public BaseResponse businessExceptionHandler(BusinessException e) {
        log.error("业务异常："+e.getMessage(), e);
        return ResultUtils.error(e.getCode(), e.getMessage(), e.getDescription());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse runtimeExceptionHandler(RuntimeException e) {
        log.error("运行时异常：", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, e.getMessage(),"运行时异常");
    }

}
