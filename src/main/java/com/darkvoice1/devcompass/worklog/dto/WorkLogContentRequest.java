package com.darkvoice1.devcompass.worklog.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 填写或编辑工作日志的内容。
 */
public class WorkLogContentRequest {

    @NotNull(message = "日志日期不能为空")
    private LocalDate logDate;

    @Size(max = 5000, message = "计划内容长度不能超过5000个字符")
    private String planContent;

    @NotBlank(message = "完成总结不能为空")
    @Size(max = 5000, message = "完成总结长度不能超过5000个字符")
    private String summaryContent;

    @NotBlank(message = "提交短哈希不能为空")
    @Size(max = 500, message = "提交短哈希长度不能超过500个字符")
    @Pattern(regexp = "(?i)[0-9a-f]{7,12}(\\s*,\\s*[0-9a-f]{7,12})*",
            message = "提交短哈希格式不正确")
    private String commitHashes;

    @NotNull(message = "实际耗时不能为空")
    @Min(value = 0, message = "实际耗时不能为负数")
    private Integer spentMinutes;

    @Size(max = 5000, message = "阻塞原因长度不能超过5000个字符")
    private String blockerReason;

    public LocalDate getLogDate() {
        return logDate;
    }

    public void setLogDate(LocalDate logDate) {
        this.logDate = logDate;
    }

    public String getPlanContent() {
        return planContent;
    }

    public void setPlanContent(String planContent) {
        this.planContent = planContent;
    }

    public String getSummaryContent() {
        return summaryContent;
    }

    public void setSummaryContent(String summaryContent) {
        this.summaryContent = summaryContent;
    }

    public String getCommitHashes() {
        return commitHashes;
    }

    public void setCommitHashes(String commitHashes) {
        this.commitHashes = commitHashes;
    }

    public Integer getSpentMinutes() {
        return spentMinutes;
    }

    public void setSpentMinutes(Integer spentMinutes) {
        this.spentMinutes = spentMinutes;
    }

    public String getBlockerReason() {
        return blockerReason;
    }

    public void setBlockerReason(String blockerReason) {
        this.blockerReason = blockerReason;
    }
}
