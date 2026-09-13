package com.darkvoice1.devcompass.task.dto;

import java.util.List;

/**
 * 任务分页查询响应数据。
 */
public class TaskPageResponse {

    private List<TaskDetailResponse> records;
    private long total;
    private long page;
    private long pageSize;
    private long totalPages;

    public List<TaskDetailResponse> getRecords() {
        return records;
    }

    public void setRecords(List<TaskDetailResponse> records) {
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
