package com.darkvoice1.devcompass.search.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 全局搜索查询参数。
 */
public class SearchQueryRequest {

    @Size(max = 200, message = "搜索关键字长度不能超过200个字符")
    private String keyword;

    private SearchItemType type;

    @Min(value = 1, message = "项目必须大于等于1")
    private Long projectId;

    private String status;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public SearchItemType getType() {
        return type;
    }

    public void setType(SearchItemType type) {
        this.type = type;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
    }
}
