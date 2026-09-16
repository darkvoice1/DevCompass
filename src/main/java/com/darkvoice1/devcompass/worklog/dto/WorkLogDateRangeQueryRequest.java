package com.darkvoice1.devcompass.worklog.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

/**
 * 按日志日期范围查询的请求参数。
 */
public class WorkLogDateRangeQueryRequest {

    @NotNull(message = "日志开始日期不能为空")
    private LocalDate logDateFrom;

    @NotNull(message = "日志结束日期不能为空")
    private LocalDate logDateTo;

    public LocalDate getLogDateFrom() {
        return logDateFrom;
    }

    public void setLogDateFrom(LocalDate logDateFrom) {
        this.logDateFrom = logDateFrom;
    }

    public LocalDate getLogDateTo() {
        return logDateTo;
    }

    public void setLogDateTo(LocalDate logDateTo) {
        this.logDateTo = logDateTo;
    }
}
