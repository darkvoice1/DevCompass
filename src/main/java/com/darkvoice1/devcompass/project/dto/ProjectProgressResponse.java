package com.darkvoice1.devcompass.project.dto;

import com.darkvoice1.devcompass.project.entity.ProgressMode;

/**
 * 项目进度接口响应数据。
 */
public class ProjectProgressResponse {

    private Long projectId;
    private ProgressMode mode;
    private Integer progress;
    private Integer autoProgress;
    private Integer manualProgress;
    private String progressReason;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public ProgressMode getMode() {
        return mode;
    }

    public void setMode(ProgressMode mode) {
        this.mode = mode;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Integer getAutoProgress() {
        return autoProgress;
    }

    public void setAutoProgress(Integer autoProgress) {
        this.autoProgress = autoProgress;
    }

    public Integer getManualProgress() {
        return manualProgress;
    }

    public void setManualProgress(Integer manualProgress) {
        this.manualProgress = manualProgress;
    }

    public String getProgressReason() {
        return progressReason;
    }

    public void setProgressReason(String progressReason) {
        this.progressReason = progressReason;
    }
}
