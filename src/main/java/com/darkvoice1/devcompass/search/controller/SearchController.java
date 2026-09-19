package com.darkvoice1.devcompass.search.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.darkvoice1.devcompass.common.web.ApiResponse;
import com.darkvoice1.devcompass.search.dto.SearchQueryRequest;
import com.darkvoice1.devcompass.search.dto.SearchResponse;
import com.darkvoice1.devcompass.search.service.SearchService;

/**
 * 提供全局关键字搜索接口。
 */
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    /**
     * 创建搜索控制器。
     *
     * @param searchService 搜索业务服务
     */
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * 按关键字搜索项目和任务。
     *
     * @param request 关键字和可选类型
     * @return 搜索结果
     */
    @GetMapping
    public ApiResponse<SearchResponse> search(@Valid @ModelAttribute SearchQueryRequest request) {
        return ApiResponse.success(searchService.search(request));
    }
}
