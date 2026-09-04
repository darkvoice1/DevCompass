package com.darkvoice1.devcompass.common.validation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 验证统一响应和字段级参数校验。
 */
@WebMvcTest(ValidationController.class)
class ValidationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证合法请求返回统一成功结构。
     */
    @Test
    void shouldReturnSuccessResponseForValidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/validation/demo")
                        .contentType("application/json")
                        .content("{\"name\":\"DevCompass\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"code\":\"0\",\"message\":\"success\",\"data\":{\"name\":\"DevCompass\"}}"));
    }

    /**
     * 验证空名称返回字段级校验错误。
     */
    @Test
    void shouldReturnFieldValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/validation/demo")
                        .contentType("application/json")
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(
                        "{\"code\":\"VALIDATION_ERROR\",\"message\":\"请求参数校验失败\",\"data\":{\"name\":\"名称不能为空\"}}"));
    }
}
