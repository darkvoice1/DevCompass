package com.darkvoice1.devcompass.common.web;

/**
 * 统一 API 响应结构。
 *
 * @param <T> 响应数据类型
 */
public class ApiResponse<T> {

    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建成功响应。
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("0", "success", data);
    }

    /**
     * 创建失败响应。
     *
     * @param code 错误码
     * @param message 错误消息
     * @param data 错误详情
     * @param <T> 错误详情类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> failure(String code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
