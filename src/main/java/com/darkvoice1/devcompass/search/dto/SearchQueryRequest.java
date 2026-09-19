package com.darkvoice1.devcompass.search.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 全局搜索查询参数。
 */
public class SearchQueryRequest {

    @NotBlank(message = "搜索关键字不能为空")
    @Size(max = 200, message = "搜索关键字长度不能超过200个字符")
    private String keyword;

    private SearchItemType type;

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
}
