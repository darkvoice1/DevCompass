package com.darkvoice1.devcompass.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.darkvoice1.devcompass.common.time.DevCompassProperties;

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

    /**
     * 验证超过上传上限时返回附件业务大小说明。
     */
    @Test
    void shouldConvertOversizedUploadToValidationError() {
        DevCompassProperties properties = new DevCompassProperties();
        properties.getStorage().setMaxSizeBytes(1024);
        GlobalExceptionHandler configured = new GlobalExceptionHandler(properties);

        var response = configured.handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(11 * 1024 * 1024));

        assertThat(response.getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getMessage()).isEqualTo("文件大小不能超过1024字节");
        assertThat(response.getData()).isNull();
    }

    /**
     * 验证缺少上传文件时返回参数错误。
     */
    @Test
    void shouldConvertMissingUploadFileToValidationError() {
        var response = handler.handleMissingUploadFile(new MissingServletRequestPartException("file"));

        assertThat(response.getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getMessage()).isEqualTo("文件不能为空");
    }
}
