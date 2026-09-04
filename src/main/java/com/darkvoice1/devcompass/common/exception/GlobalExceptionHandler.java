package com.darkvoice1.devcompass.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.darkvoice1.devcompass.common.web.ApiResponse;

/**
 * 统一处理业务异常、参数异常和未知异常。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

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

}
