package com.darkvoice1.devcompass.task.dto;

import com.darkvoice1.devcompass.task.entity.TaskStatus;

import jakarta.validation.constraints.NotNull;

/**
 * 变更任务状态的请求参数。
 */
public class ChangeTaskStatusRequest {

    @NotNull(message = "目标状态不能为空")
    private TaskStatus targetStatus;

    public TaskStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(TaskStatus targetStatus) {
        this.targetStatus = targetStatus;
    }
}
