package com.darkvoice1.devcompass.activity.dto;

import java.util.List;

/**
 * 项目动态分页查询响应。
 */
public class ActivityPageResponse {

    private List<ActivityResponse> records;

    private long total;

    private long page;

    private long pageSize;

    private long totalPages;

    public List<ActivityResponse> getRecords() {
        return records;
    }

    public void setRecords(List<ActivityResponse> records) {
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }
}
