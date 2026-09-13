package com.darkvoice1.devcompass.task.dto;

import com.darkvoice1.devcompass.task.entity.TaskPriority;
import com.darkvoice1.devcompass.task.entity.TaskStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 任务分页查询的请求参数。
 */
public class TaskPageQueryRequest {

    private static final long MAX_PAGE = 1_000_000L;
    private static final int MAX_PAGE_SIZE = 100;

    @NotNull(message = "项目不能为空")
    private Long projectId;

    @Min(value = 1, message = "页码必须大于等于1")
    @Max(value = MAX_PAGE, message = "页码不能超过1000000")
    private Long page = 1L;

    @Min(value = 1, message = "每页数量必须大于等于1")
    @Max(value = MAX_PAGE_SIZE, message = "每页数量不能超过100")
    private Integer pageSize = 20;

    private TaskStatus status;
    private TaskPriority priority;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
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
}
