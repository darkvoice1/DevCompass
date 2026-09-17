package com.darkvoice1.devcompass.dashboard.dto;

import com.darkvoice1.devcompass.project.entity.ProjectStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 仪表盘项目筛选参数。
 */
public class DashboardProjectQueryRequest {

    private ProjectStatus status;

    @Size(max = 500, message = "项目标签长度不能超过500个字符")
    private String tag;

    @Min(value = 1, message = "最近活跃天数必须大于等于1")
    private Integer activeWithinDays;

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Integer getActiveWithinDays() {
        return activeWithinDays;
    }

    public void setActiveWithinDays(Integer activeWithinDays) {
        this.activeWithinDays = activeWithinDays;
    }
}
