package com.darkvoice1.devcompass.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.common.web.ApiResponse;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 将未认证和 Token 错误转换为统一 JSON 响应。
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 根据失败原因返回统一认证错误。
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authenticationException) throws IOException, ServletException {
        ErrorCode errorCode = errorCode(request, authenticationException);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                ApiResponse.failure(errorCode.getCode(), errorCode.getMessage(), null));
    }

    private ErrorCode errorCode(HttpServletRequest request, AuthenticationException exception) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            return ErrorCode.UNAUTHENTICATED;
        }
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof JwtValidationException
                    && cause.getMessage() != null
                    && cause.getMessage().toLowerCase().contains("expired")) {
                return ErrorCode.ACCESS_TOKEN_EXPIRED;
            }
            cause = cause.getCause();
        }
        return ErrorCode.INVALID_ACCESS_TOKEN;
    }
}
