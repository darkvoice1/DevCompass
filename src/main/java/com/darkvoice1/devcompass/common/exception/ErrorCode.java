package com.darkvoice1.devcompass.common.exception;

/**
 * 定义通用错误码。
 */
public enum ErrorCode {

    /** 业务规则错误。 */
    BUSINESS_ERROR("BUSINESS_ERROR", "业务处理失败"),
    /** 请求参数校验错误。 */
    VALIDATION_ERROR("VALIDATION_ERROR", "请求参数校验失败"),
    /** 用户名或密码错误。 */
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "用户名或密码错误"),
    /** 请求没有提供有效身份。 */
    UNAUTHENTICATED("UNAUTHENTICATED", "请先登录"),
    /** Access Token 无效。 */
    INVALID_ACCESS_TOKEN("INVALID_ACCESS_TOKEN", "Access Token 无效"),
    /** Access Token 已过期。 */
    ACCESS_TOKEN_EXPIRED("ACCESS_TOKEN_EXPIRED", "Access Token 已过期"),
    /** 未知系统错误。 */
    INTERNAL_ERROR("INTERNAL_ERROR", "系统内部错误");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
