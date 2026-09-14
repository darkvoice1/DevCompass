package com.darkvoice1.devcompass.project.dto;

import com.darkvoice1.devcompass.project.entity.ProgressMode;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 切换项目进度模式或提交人工校准值的请求参数。
 */
public class UpdateProjectProgressRequest {

    @NotNull(message = "进度模式不能为空")
    private ProgressMode mode;

    @Min(value = 0, message = "人工进度不能小于0")
    @Max(value = 100, message = "人工进度不能大于100")
    private Integer manualProgress;

    @Size(max = 500, message = "校准原因长度不能超过500个字符")
    private String progressReason;

    public ProgressMode getMode() {
        return mode;
    }

    public void setMode(ProgressMode mode) {
        this.mode = mode;
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
