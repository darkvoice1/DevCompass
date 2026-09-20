package com.darkvoice1.devcompass.timeline.dto;

import java.time.LocalDate;

/**
 * 时间线查询参数。
 */
public class TimelineQueryRequest {

    private LocalDate fromDate;

    private LocalDate toDate;

    private TimelineView view;

    private LocalDate date;

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

    public TimelineView getView() {
        return view;
    }

    public void setView(TimelineView view) {
        this.view = view;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
