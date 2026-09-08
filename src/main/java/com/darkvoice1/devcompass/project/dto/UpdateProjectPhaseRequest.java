package com.darkvoice1.devcompass.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 编辑项目阶段的请求参数。
 */
public class UpdateProjectPhaseRequest {

    @NotBlank(message = "阶段名称不能为空")
    @Size(max = 200, message = "阶段名称长度不能超过200个字符")
    private String name;

    @Size(max = 2000, message = "阶段描述长度不能超过2000个字符")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
