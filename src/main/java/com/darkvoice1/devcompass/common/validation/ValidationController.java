package com.darkvoice1.devcompass.common.validation;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.darkvoice1.devcompass.common.web.ApiResponse;

/**
 * 提供参数校验闭环示例接口。
 */
@RestController
@RequestMapping("/api/v1/validation")
public class ValidationController {

    /**
     * 接收并返回通过校验的名称。
     *
     * @param request 校验请求
     * @return 统一成功响应
     */
    @PostMapping("/demo")
    public ApiResponse<ValidationRequest> validate(@Valid @RequestBody ValidationRequest request) {
        return ApiResponse.success(request);
    }
}
