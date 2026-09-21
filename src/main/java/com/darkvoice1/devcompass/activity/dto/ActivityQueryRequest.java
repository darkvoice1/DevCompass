package com.darkvoice1.devcompass.activity.dto;

import java.time.LocalDate;

import com.darkvoice1.devcompass.activity.entity.ActivityObjectType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 项目动态分页查询参数。
 */
public class ActivityQueryRequest {

    private static final long MAX_PAGE = 1_000_000L;
    private static final int MAX_PAGE_SIZE = 100;

    @NotNull(message = "项目不能为空")
    @Min(value = 1, message = "项目必须大于等于1")
    private Long projectId;

    private ActivityObjectType objectType;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    @Min(value = 1, message = "页码必须大于等于1")
    @Max(value = MAX_PAGE, message = "页码不能超过1000000")
    private Long page = 1L;

    @Min(value = 1, message = "每页数量必须大于等于1")
    @Max(value = MAX_PAGE_SIZE, message = "每页数量不能超过100")
    private Integer pageSize = 20;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public ActivityObjectType getObjectType() {
        return objectType;
    }

    public void setObjectType(ActivityObjectType objectType) {
        this.objectType = objectType;
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

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
