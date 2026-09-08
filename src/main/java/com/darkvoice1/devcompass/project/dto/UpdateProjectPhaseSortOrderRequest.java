package com.darkvoice1.devcompass.project.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 调整项目阶段排序的请求参数。
 */
public class UpdateProjectPhaseSortOrderRequest {

    @NotNull(message = "排序序号不能为空")
    @Min(value = 0, message = "排序序号不能小于0")
    private Integer sortOrder;

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
