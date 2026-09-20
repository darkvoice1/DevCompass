package com.darkvoice1.devcompass.search.dto;

import java.time.Instant;

/**
 * 一条搜索结果，用于跳转到项目或任务。
 */
public class SearchItemResponse {

    private SearchItemType type;

    private Long id;

    private String title;

    private String summary;

    private Instant updatedAt;

    private Long projectId;

    private String projectName;

    private Long taskId;

    public SearchItemType getType() {
        return type;
    }

    public void setType(SearchItemType type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
}
