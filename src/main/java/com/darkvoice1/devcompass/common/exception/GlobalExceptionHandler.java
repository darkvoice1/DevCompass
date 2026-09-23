package com.darkvoice1.devcompass.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.darkvoice1.devcompass.common.time.DevCompassProperties;
import com.darkvoice1.devcompass.common.web.ApiResponse;

/**
 * 统一处理业务异常、参数异常和未知异常。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final long ONE_MB = 1024L * 1024;

    private final DevCompassProperties properties;

    /**
     * 按应用配置创建异常处理器。切片测试没有这份配置时，使用默认上限。
     *
     * @param propertiesProvider 应用自定义配置，可以不存在
     */
    @Autowired
    public GlobalExceptionHandler(ObjectProvider<DevCompassProperties> propertiesProvider) {
        this(propertiesProvider.getIfAvailable());
    }

    /**
     * 按给定配置创建异常处理器。
     *
     * @param properties 应用自定义配置，为空时使用默认值
     */
    public GlobalExceptionHandler(DevCompassProperties properties) {
        this.properties = properties == null ? new DevCompassProperties() : properties;
    }

    /**
     * 使用默认配置创建异常处理器，便于不启动 Spring 的测试。
     */
    public GlobalExceptionHandler() {
        this(new DevCompassProperties());
    }

    /**
     * 处理业务规则异常。
     *
     * @param exception 业务异常
     * @return 业务错误响应
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        return ApiResponse.failure(
                exception.getErrorCode().getCode(), exception.getMessage(), null);
    }

    /**
     * 处理请求体字段校验异常。
     *
     * @param exception 参数校验异常
     * @return 包含字段错误的响应
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ApiResponse.failure(
                ErrorCode.VALIDATION_ERROR.getCode(), ErrorCode.VALIDATION_ERROR.getMessage(), errors);
    }

    /**
     * 上传文件超过 Spring 接收上限时，仍返回附件的业务大小说明。
     *
     * @param exception 上传大小异常
     * @return 参数错误响应
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        return ApiResponse.failure(
                ErrorCode.VALIDATION_ERROR.getCode(), maxSizeMessage(), null);
    }

    /**
     * 没有提交上传文件时，返回和空文件相同的提示。
     *
     * @param exception 缺少上传部分的异常
     * @return 参数错误响应
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ApiResponse<Void> handleMissingUploadFile(MissingServletRequestPartException exception) {
        return ApiResponse.failure(
                ErrorCode.VALIDATION_ERROR.getCode(), "文件不能为空", null);
    }

    /**
     * 处理未预期的系统异常。
     *
     * @param exception 未知异常
     * @return 系统错误响应
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnknownException(Exception exception) {
        return ApiResponse.failure(
                ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage(), null);
    }

    /**
     * 把配置的字节上限说成和存储服务一致的大小说明。
     *
     * @return 给调用方看的大小说明
     */
    private String maxSizeMessage() {
        long maxSizeBytes = 10L * ONE_MB;
        if (properties.getStorage() != null && properties.getStorage().getMaxSizeBytes() > 0) {
            maxSizeBytes = properties.getStorage().getMaxSizeBytes();
        }
        if (maxSizeBytes % ONE_MB == 0) {
            return "文件大小不能超过" + (maxSizeBytes / ONE_MB) + "MB";
        }
        return "文件大小不能超过" + maxSizeBytes + "字节";
    }

}
