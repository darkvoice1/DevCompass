package com.darkvoice1.devcompass.timeline.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 时间线查询结果。
 */
public class TimelineResponse {

    private LocalDate fromDate;

    private LocalDate toDate;

    private List<TimelineEventResponse> items;

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

    public List<TimelineEventResponse> getItems() {
        return items;
    }

    public void setItems(List<TimelineEventResponse> items) {
        this.items = items;
    }
}
