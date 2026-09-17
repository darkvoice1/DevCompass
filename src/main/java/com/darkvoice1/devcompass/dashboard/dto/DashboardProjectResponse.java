package com.darkvoice1.devcompass.dashboard.dto;

import java.time.Instant;

import com.darkvoice1.devcompass.project.entity.ProjectStatus;

/**
 * 仪表盘中的单个项目摘要。
 */
public class DashboardProjectResponse {

    private Long id;

    private String name;

    private ProjectStatus status;

    private Integer progress;

    private String tags;

    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
