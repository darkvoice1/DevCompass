package com.darkvoice1.devcompass.timeline.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

/**
 * 时间线查询参数。
 */
public class TimelineQueryRequest {

    @NotNull(message = "开始日期不能为空")
    private LocalDate fromDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate toDate;

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }
}
