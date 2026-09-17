package com.darkvoice1.devcompass.dashboard.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.darkvoice1.devcompass.common.web.ApiResponse;
import com.darkvoice1.devcompass.dashboard.dto.DashboardOverviewResponse;
import com.darkvoice1.devcompass.dashboard.dto.DashboardProjectQueryRequest;
import com.darkvoice1.devcompass.dashboard.service.DashboardService;

/**
 * 提供多项目仪表盘查询接口。
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 创建仪表盘控制器。
     *
     * @param dashboardService 仪表盘业务服务
     */
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 查询多项目仪表盘摘要。
     *
     * @param request 项目筛选参数
     * @return 仪表盘聚合结果
     */
    @GetMapping("/projects")
    public ApiResponse<DashboardOverviewResponse> getProjectOverview(
            @Valid @ModelAttribute DashboardProjectQueryRequest request) {
        return ApiResponse.success(dashboardService.getProjectOverview(request));
    }
}
