package com.darkvoice1.devcompass.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 验证统一异常处理器的错误码映射。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /**
     * 验证未知异常转换为统一系统错误。
     */
    @Test
    void shouldConvertUnknownExceptionToInternalError() {
        var response = handler.handleUnknownException(new IllegalStateException("unexpected"));

        assertThat(response.getCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getMessage()).isEqualTo("系统内部错误");
        assertThat(response.getData()).isNull();
    }
}
