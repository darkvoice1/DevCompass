package com.darkvoice1.devcompass.timeline.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.darkvoice1.devcompass.common.web.ApiResponse;
import com.darkvoice1.devcompass.timeline.dto.TimelineQueryRequest;
import com.darkvoice1.devcompass.timeline.dto.TimelineResponse;
import com.darkvoice1.devcompass.timeline.service.TimelineService;

/**
 * 提供项目时间线查询接口。
 */
@RestController
@RequestMapping("/api/v1/timeline")
public class TimelineController {

    private final TimelineService timelineService;

    /**
     * 创建时间线控制器。
     *
     * @param timelineService 时间线业务服务
     */
    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    /**
     * 按日期范围查询时间线事件。
     *
     * @param request 开始日期和结束日期
     * @return 时间线事件
     */
    @GetMapping
    public ApiResponse<TimelineResponse> getTimeline(
            @Valid @ModelAttribute TimelineQueryRequest request) {
        return ApiResponse.success(timelineService.getTimeline(request));
    }
}
