package com.darkvoice1.devcompass.common.exception;

/**
 * 表示可预期的业务规则异常。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 创建业务异常。
     *
     * @param errorCode 错误码
     * @param message 具体错误消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
