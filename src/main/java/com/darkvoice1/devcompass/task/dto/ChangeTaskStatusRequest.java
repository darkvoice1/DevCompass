package com.darkvoice1.devcompass.task.dto;

import com.darkvoice1.devcompass.task.entity.TaskStatus;
import com.darkvoice1.devcompass.worklog.dto.WorkLogContentRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 变更任务状态的请求参数。
 */
public class ChangeTaskStatusRequest {

    @NotNull(message = "目标状态不能为空")
    private TaskStatus targetStatus;

    @Valid
    private WorkLogContentRequest completionLog;

    public TaskStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(TaskStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public WorkLogContentRequest getCompletionLog() {
        return completionLog;
    }

    public void setCompletionLog(WorkLogContentRequest completionLog) {
        this.completionLog = completionLog;
    }
}
