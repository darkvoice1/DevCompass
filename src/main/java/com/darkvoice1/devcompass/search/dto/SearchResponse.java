package com.darkvoice1.devcompass.search.dto;

import java.util.List;

/**
 * 全局搜索查询结果。
 */
public class SearchResponse {

    private String keyword;

    private SearchItemType type;

    private List<SearchItemResponse> items;

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

    public List<SearchItemResponse> getItems() {
        return items;
    }

    public void setItems(List<SearchItemResponse> items) {
        this.items = items;
    }
}
