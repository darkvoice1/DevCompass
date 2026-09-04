package com.darkvoice1.devcompass.common.validation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 参数校验示例请求。
 */
public class ValidationRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 50, message = "名称长度不能超过50个字符")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
