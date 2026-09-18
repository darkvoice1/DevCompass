package com.darkvoice1.devcompass.dashboard.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.darkvoice1.devcompass.project.entity.ProgressMode;
import com.darkvoice1.devcompass.project.entity.ProjectStatus;

/**
 * 仪表盘项目聚合查询结果，包含健康度计算所需数据。
 */
public class DashboardProjectRow {

    private Long id;

    private String name;

    private ProjectStatus status;

    private ProgressMode progressMode;

    private Integer autoProgress;

    private Integer manualProgress;

    private String tags;

    private LocalDate targetDate;

    private long overdueTaskCount;

    private long dueSoonTaskCount;

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

    public ProgressMode getProgressMode() {
        return progressMode;
    }

    public void setProgressMode(ProgressMode progressMode) {
        this.progressMode = progressMode;
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

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public long getOverdueTaskCount() {
        return overdueTaskCount;
    }

    public void setOverdueTaskCount(long overdueTaskCount) {
        this.overdueTaskCount = overdueTaskCount;
    }

    public long getDueSoonTaskCount() {
        return dueSoonTaskCount;
    }

    public void setDueSoonTaskCount(long dueSoonTaskCount) {
        this.dueSoonTaskCount = dueSoonTaskCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
