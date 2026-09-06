package com.darkvoice1.devcompass.project.dto;

import java.time.LocalDate;

import com.darkvoice1.devcompass.project.entity.ProjectStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 编辑项目的请求参数。
 */
public class UpdateProjectRequest {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 200, message = "项目名称长度不能超过200个字符")
    private String name;

    @Size(max = 2000, message = "项目描述长度不能超过2000个字符")
    private String description;

    private ProjectStatus status;

    private LocalDate targetDate;

    @Size(max = 500, message = "技术栈长度不能超过500个字符")
    private String techStack;

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

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public String getTechStack() {
        return techStack;
    }

    public void setTechStack(String techStack) {
        this.techStack = techStack;
    }
}
