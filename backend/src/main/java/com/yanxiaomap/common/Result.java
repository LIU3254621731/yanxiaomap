package com.yanxiaomap.common;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一API响应结果封装类
 * 遵循格式: { success: boolean, message?: string, data?: any, code?: string }
 */
@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    // 是否成功
    private boolean success;

    // 响应消息
    private String message;

    // 响应数据
    private T data;

    // 错误码
    private String code;

    // 时间戳
    private long timestamp = System.currentTimeMillis();

    /**
     * 私有构造方法
     */
    private Result() {
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        return result;
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = success();
        result.setData(data);
        return result;
    }

    /**
     * 成功响应（带消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = success(data);
        result.setMessage(message);
        return result;
    }

    /**
     * 失败响应（错误码和消息）
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        return result;
    }

    /**
     * 失败响应（自定义消息）
     */
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode(ResultCode.SYSTEM_ERROR.getCode());
        result.setMessage(message);
        return result;
    }

    /**
     * 失败响应（错误码和自定义消息）
     */
    public static <T> Result<T> error(ResultCode resultCode, String message) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode(resultCode.getCode());
        result.setMessage(message);
        return result;
    }

    /**
     * 失败响应（错误码、消息和数据）
     */
    public static <T> Result<T> error(ResultCode resultCode, String message, T data) {
        Result<T> result = error(resultCode, message);
        result.setData(data);
        return result;
    }
}