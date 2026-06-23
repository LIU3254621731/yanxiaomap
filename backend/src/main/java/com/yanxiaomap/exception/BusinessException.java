package com.yanxiaomap.exception;

import com.yanxiaomap.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常类
 * 用于处理业务逻辑中的异常情况
 */
@Getter
public class BusinessException extends RuntimeException {
    private final ResultCode resultCode;

    public BusinessException() {
        super(ResultCode.BUSINESS_ERROR.getMessage());
        this.resultCode = ResultCode.BUSINESS_ERROR;
    }

    public BusinessException(String message) {
        super(message);
        this.resultCode = ResultCode.BUSINESS_ERROR;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.resultCode = ResultCode.BUSINESS_ERROR;
    }
}