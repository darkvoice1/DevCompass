package com.darkvoice1.devcompass.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 首页焦点清单查询结果。
 */
public class FocusListResponse {

    private FocusListType type;

    private LocalDate fromDate;

    private LocalDate toDate;

    private List<FocusListItemResponse> items;

    public FocusListType getType() {
        return type;
    }

    public void setType(FocusListType type) {
        this.type = type;
    }

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

    public List<FocusListItemResponse> getItems() {
        return items;
    }

    public void setItems(List<FocusListItemResponse> items) {
        this.items = items;
    }
}
