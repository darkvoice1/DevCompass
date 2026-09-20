package com.darkvoice1.devcompass.timeline.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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
import com.darkvoice1.devcompass.timeline.dto.TimelineView;
import com.darkvoice1.devcompass.timeline.repository.TimelineMapper;

/**
 * 按日期范围查询时间线事件。
 */
@Service
public class TimelineService {

    private final TimelineMapper timelineMapper;

    private final Clock clock;

    /**
     * 创建时间线服务。
     *
     * @param timelineMapper 时间线数据访问对象
     * @param clock 应用时钟，用于按统一时区计算今天
     */
    public TimelineService(TimelineMapper timelineMapper, Clock clock) {
        this.timelineMapper = timelineMapper;
        this.clock = clock;
    }

    /**
     * 查询指定日期范围内有截止日期的任务，以及有目标日期的项目。
     *
     * @param request 日期范围或周/月视图
     * @return 时间线事件
     */
    public TimelineResponse getTimeline(TimelineQueryRequest request) {
        LocalDate fromDate;
        LocalDate toDate;
        if (request.getFromDate() != null && request.getToDate() != null) {
            fromDate = request.getFromDate();
            toDate = request.getToDate();
        } else if (request.getView() != null) {
            LocalDate anchor = request.getDate() != null ? request.getDate() : LocalDate.now(clock);
            fromDate = startOfView(anchor, request.getView());
            toDate = endOfView(anchor, request.getView());
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请提供日期范围或视图类型");
        }
        if (fromDate.isAfter(toDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "日期范围不合法");
        }
        List<TimelineEventResponse> items = new ArrayList<>(timelineMapper.selectTaskEvents(
                fromDate, toDate));
        items.addAll(timelineMapper.selectProjectEvents(fromDate, toDate));
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
        response.setFromDate(fromDate);
        response.setToDate(toDate);
        response.setItems(items);
        return response;
    }

    /**
     * 计算周或月视图的起始日期。
     */
    private LocalDate startOfView(LocalDate date, TimelineView view) {
        return view == TimelineView.WEEK
                ? date.with(DayOfWeek.MONDAY)
                : date.withDayOfMonth(1);
    }

    /**
     * 计算周或月视图的结束日期。
     */
    private LocalDate endOfView(LocalDate date, TimelineView view) {
        return view == TimelineView.WEEK
                ? date.with(DayOfWeek.SUNDAY)
                : date.with(TemporalAdjusters.lastDayOfMonth());
    }
}
