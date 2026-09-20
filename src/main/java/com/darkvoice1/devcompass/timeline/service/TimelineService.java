package com.darkvoice1.devcompass.timeline.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.task.entity.TaskStatus;
import com.darkvoice1.devcompass.timeline.dto.TimelineEventResponse;
import com.darkvoice1.devcompass.timeline.dto.TimelineEventType;
import com.darkvoice1.devcompass.timeline.dto.TimelineQueryRequest;
import com.darkvoice1.devcompass.timeline.dto.TimelineResponse;
import com.darkvoice1.devcompass.timeline.repository.TimelineMapper;

/**
 * 按日期范围查询时间线事件。
 */
@Service
public class TimelineService {

    private final TimelineMapper timelineMapper;

    /**
     * 创建时间线服务。
     *
     * @param timelineMapper 时间线数据访问对象
     */
    public TimelineService(TimelineMapper timelineMapper) {
        this.timelineMapper = timelineMapper;
    }

    /**
     * 查询指定日期范围内有截止日期的任务，以及有目标日期的项目。
     *
     * @param request 开始日期和结束日期
     * @return 时间线事件
     */
    public TimelineResponse getTimeline(TimelineQueryRequest request) {
        if (request.getFromDate().isAfter(request.getToDate())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "日期范围不合法");
        }
        List<TimelineEventResponse> items = new ArrayList<>(timelineMapper.selectTaskEvents(
                request.getFromDate(), request.getToDate()));
        items.addAll(timelineMapper.selectProjectEvents(
                request.getFromDate(), request.getToDate()));
        items.forEach(item -> {
            if (item.getType() == TimelineEventType.TASK) {
                item.setCompleted(item.getStatus() == TaskStatus.COMPLETED);
            }
        });
        items.sort(Comparator
                .comparing((TimelineEventResponse item) -> item.getDate(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(item -> item.getType().name())
                .thenComparing(item -> item.getId(),
                        Comparator.nullsLast(Comparator.naturalOrder())));

        TimelineResponse response = new TimelineResponse();
        response.setFromDate(request.getFromDate());
        response.setToDate(request.getToDate());
        response.setItems(items);
        return response;
    }
}
