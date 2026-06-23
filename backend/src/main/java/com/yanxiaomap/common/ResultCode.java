package com.yanxiaomap.common;

import lombok.Getter;

/**
 * 响应码枚举
 * 定义系统所有响应码和对应消息
 */
@Getter
public enum ResultCode {
    // 成功
    SUCCESS("00000", "操作成功"),

    // 客户端错误
    BAD_REQUEST("A0400", "请求参数错误"),
    UNAUTHORIZED("A0401", "未授权访问"),
    FORBIDDEN("A0403", "禁止访问"),
    NOT_FOUND("A0404", "资源不存在"),
    METHOD_NOT_ALLOWED("A0405", "请求方法不允许"),
    REQUEST_TIMEOUT("A0408", "请求超时"),
    TOO_MANY_REQUESTS("A0429", "请求过于频繁"),
    PARAM_ERROR("A0410", "参数错误"),

    // 业务错误
    BUSINESS_ERROR("B0001", "业务处理失败"),
    DATA_NOT_EXIST("B0002", "数据不存在"),
    DATA_ALREADY_EXIST("B0003", "数据已存在"),
    DATA_VALIDATION_FAILED("B0004", "数据验证失败"),
    OPERATION_NOT_ALLOWED("B0005", "操作不允许"),

    // 系统错误
    SYSTEM_ERROR("C0001", "系统内部错误"),
    SERVICE_UNAVAILABLE("C0002", "服务不可用"),
    DATABASE_ERROR("C0003", "数据库错误"),
    CACHE_ERROR("C0004", "缓存错误"),
    NETWORK_ERROR("C0005", "网络错误"),

    // 第三方服务错误
    THIRD_PARTY_ERROR("D0001", "第三方服务错误"),
    MAP_API_ERROR("D0002", "地图API服务错误"),
    FILE_UPLOAD_ERROR("D0003", "文件上传错误"),
    FILE_DOWNLOAD_ERROR("D0004", "文件下载错误");

    // 响应码
    private final String code;

    // 响应消息
    private final String message;

    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据code获取枚举
     */
    public static ResultCode getByCode(String code) {
        for (ResultCode resultCode : values()) {
            if (resultCode.getCode().equals(code)) {
                return resultCode;
            }
        }
        return SYSTEM_ERROR;
    }
}