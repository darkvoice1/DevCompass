package com.darkvoice1.devcompass.dashboard.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 首页焦点清单查询参数。
 */
public class FocusListQueryRequest {

    @NotNull(message = "清单类型不能为空")
    private FocusListType type;

    public FocusListType getType() {
        return type;
    }

    public void setType(FocusListType type) {
        this.type = type;
    }
}
