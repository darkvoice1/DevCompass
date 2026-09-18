package com.darkvoice1.devcompass.dashboard.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.darkvoice1.devcompass.common.web.ApiResponse;
import com.darkvoice1.devcompass.dashboard.dto.FocusListQueryRequest;
import com.darkvoice1.devcompass.dashboard.dto.FocusListResponse;
import com.darkvoice1.devcompass.dashboard.service.FocusListService;

/**
 * 提供首页焦点清单查询接口。
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class FocusListController {

    private final FocusListService focusListService;

    /**
     * 创建焦点清单控制器。
     *
     * @param focusListService 焦点清单业务服务
     */
    public FocusListController(FocusListService focusListService) {
        this.focusListService = focusListService;
    }

    /**
     * 查询首页焦点清单。
     *
     * @param request 清单类型
     * @return 日期范围和任务列表
     */
    @GetMapping("/focus-lists")
    public ApiResponse<FocusListResponse> getFocusList(
            @Valid @ModelAttribute FocusListQueryRequest request) {
        return ApiResponse.success(focusListService.getFocusList(request));
    }
}
