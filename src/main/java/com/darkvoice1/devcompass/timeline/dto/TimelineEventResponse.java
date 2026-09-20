package com.darkvoice1.devcompass.timeline.dto;

import java.time.LocalDate;

import com.darkvoice1.devcompass.task.entity.TaskPriority;
import com.darkvoice1.devcompass.task.entity.TaskStatus;

/**
 * 时间线上的一条事件，供前端绘制排期。
 */
public class TimelineEventResponse {

    private TimelineEventType type;

    private Long id;

    private String title;

    private LocalDate date;

    private Long projectId;

    private String projectName;

    private TaskStatus status;

    private TaskPriority priority;

    private boolean completed;

    public TimelineEventType getType() {
        return type;
    }

    public void setType(TimelineEventType type) {
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
