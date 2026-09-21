package com.darkvoice1.devcompass.activity.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.darkvoice1.devcompass.activity.dto.ActivityPageResponse;
import com.darkvoice1.devcompass.activity.dto.ActivityQueryRequest;
import com.darkvoice1.devcompass.activity.service.ActivityService;
import com.darkvoice1.devcompass.common.web.ApiResponse;

/**
 * 提供项目动态查询接口。
 */
@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {

    private final ActivityService activityService;

    /**
     * 创建动态控制器。
     *
     * @param activityService 动态业务服务
     */
    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * 按项目分页查询动态，可按对象类型和日期筛选。
     *
     * @param request 查询参数
     * @return 动态分页结果
     */
    @GetMapping
    public ApiResponse<ActivityPageResponse> queryActivities(
            @Valid @ModelAttribute ActivityQueryRequest request) {
        return ApiResponse.success(activityService.queryActivities(request));
    }
}
