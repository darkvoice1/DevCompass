package com.darkvoice1.devcompass.dashboard.dto;

import java.time.LocalDate;

import com.darkvoice1.devcompass.task.entity.TaskStatus;

/**
 * 首页焦点清单中的一条记录，可能是任务或延期项目。
 */
public class FocusListItemResponse {

    private FocusListItemKind itemKind;

    private Long taskId;

    private String title;

    private TaskStatus status;

    private LocalDate dueDate;

    private Long projectId;

    private String projectName;

    public FocusListItemKind getItemKind() {
        return itemKind;
    }

    public void setItemKind(FocusListItemKind itemKind) {
        this.itemKind = itemKind;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
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
}
