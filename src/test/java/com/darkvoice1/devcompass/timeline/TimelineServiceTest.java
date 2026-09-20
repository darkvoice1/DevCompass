package com.darkvoice1.devcompass.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.task.entity.TaskPriority;
import com.darkvoice1.devcompass.task.entity.TaskStatus;
import com.darkvoice1.devcompass.timeline.dto.TimelineEventResponse;
import com.darkvoice1.devcompass.timeline.dto.TimelineEventType;
import com.darkvoice1.devcompass.timeline.dto.TimelineQueryRequest;
import com.darkvoice1.devcompass.timeline.dto.TimelineView;
import com.darkvoice1.devcompass.timeline.repository.TimelineMapper;
import com.darkvoice1.devcompass.timeline.service.TimelineService;

/**
 * 验证时间线日期范围和完成标记。
 */
class TimelineServiceTest {

    private TimelineMapper timelineMapper;

    private TimelineService timelineService;

    /**
     * 初始化时间线服务及 Mapper 模拟对象。
     */
    @BeforeEach
    void setUp() {
        timelineMapper = mock(TimelineMapper.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-16T08:00:00+08:00"),
                ZoneId.of("Asia/Shanghai"));
        timelineService = new TimelineService(timelineMapper, clock);
    }

    /**
     * 验证按日期范围查询任务，并把已完成状态标成 completed。
     */
    @Test
    void shouldQueryTaskEventsAndMarkCompleted() {
        TimelineEventResponse todo = event(1L, "待办任务", LocalDate.of(2026, 9, 16),
                TaskStatus.TODO);
        TimelineEventResponse done = event(2L, "已完成任务", LocalDate.of(2026, 9, 20),
                TaskStatus.COMPLETED);
        when(timelineMapper.selectTaskEvents(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(todo, done));
        when(timelineMapper.selectProjectEvents(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of());

        var response = timelineService.getTimeline(request(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)));

        assertThat(response.getFromDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.getToDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(response.getItems()).extracting(item -> item.getTitle())
                .containsExactly("待办任务", "已完成任务");
        assertThat(response.getItems()).extracting(item -> item.isCompleted())
                .containsExactly(false, true);
        verify(timelineMapper).selectTaskEvents(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        verify(timelineMapper).selectProjectEvents(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        verifyNoMoreInteractions(timelineMapper);
    }

    /**
     * 验证项目目标日期会并入时间线，已完成项目标成 completed，并按日期排序。
     */
    @Test
    void shouldMergeProjectTargetDatesAndSortByDate() {
        TimelineEventResponse laterTask = event(2L, "较晚任务", LocalDate.of(2026, 9, 20),
                TaskStatus.TODO);
        TimelineEventResponse project = projectEvent(8L, "研发罗盘", LocalDate.of(2026, 9, 10), true);
        when(timelineMapper.selectTaskEvents(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(laterTask));
        when(timelineMapper.selectProjectEvents(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(project));

        var response = timelineService.getTimeline(request(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)));

        assertThat(response.getItems()).extracting(item -> item.getType())
                .containsExactly(TimelineEventType.PROJECT, TimelineEventType.TASK);
        assertThat(response.getItems()).extracting(item -> item.getTitle())
                .containsExactly("研发罗盘", "较晚任务");
        assertThat(response.getItems().get(0).isCompleted()).isTrue();
        verify(timelineMapper).selectTaskEvents(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        verify(timelineMapper).selectProjectEvents(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        verifyNoMoreInteractions(timelineMapper);
    }

    /**
     * 验证开始日期晚于结束日期时返回校验错误。
     */
    @Test
    void shouldRejectInvalidDateRange() {
        assertThatThrownBy(() -> timelineService.getTimeline(request(
                LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("日期范围不合法");
        verifyNoMoreInteractions(timelineMapper);
    }

    /**
     * 验证按周视图时使用该日所在周一到周日。
     */
    @Test
    void shouldResolveWeekViewToMondayThroughSunday() {
        when(timelineMapper.selectTaskEvents(
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20)))
                .thenReturn(List.of());
        when(timelineMapper.selectProjectEvents(
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20)))
                .thenReturn(List.of());

        TimelineQueryRequest request = new TimelineQueryRequest();
        request.setView(TimelineView.WEEK);
        request.setDate(LocalDate.of(2026, 9, 16));

        var response = timelineService.getTimeline(request);

        assertThat(response.getFromDate()).isEqualTo(LocalDate.of(2026, 9, 14));
        assertThat(response.getToDate()).isEqualTo(LocalDate.of(2026, 9, 20));
        verify(timelineMapper).selectTaskEvents(
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20));
        verify(timelineMapper).selectProjectEvents(
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20));
    }

    /**
     * 验证锚点是周日时，周视图仍落在该周周一到周日，不会跨到下一周。
     */
    @Test
    void shouldResolveWeekViewWhenAnchorIsSunday() {
        when(timelineMapper.selectTaskEvents(
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20)))
                .thenReturn(List.of());
        when(timelineMapper.selectProjectEvents(
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20)))
                .thenReturn(List.of());

        TimelineQueryRequest request = new TimelineQueryRequest();
        request.setView(TimelineView.WEEK);
        request.setDate(LocalDate.of(2026, 9, 20));

        var response = timelineService.getTimeline(request);

        assertThat(response.getFromDate()).isEqualTo(LocalDate.of(2026, 9, 14));
        assertThat(response.getToDate()).isEqualTo(LocalDate.of(2026, 9, 20));
    }

    /**
     * 验证按月视图时使用该日所在月的 1 号到月末。
     */
    @Test
    void shouldResolveMonthViewToFirstThroughLastDay() {
        when(timelineMapper.selectTaskEvents(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of());
        when(timelineMapper.selectProjectEvents(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of());

        TimelineQueryRequest request = new TimelineQueryRequest();
        request.setView(TimelineView.MONTH);
        request.setDate(LocalDate.of(2026, 9, 16));

        var response = timelineService.getTimeline(request);

        assertThat(response.getFromDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.getToDate()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    /**
     * 验证未指定 date 时，周视图按应用时区的今天所在周计算。
     */
    @Test
    void shouldUseTodayWhenViewDateIsMissing() {
        when(timelineMapper.selectTaskEvents(
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20)))
                .thenReturn(List.of());
        when(timelineMapper.selectProjectEvents(
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20)))
                .thenReturn(List.of());

        TimelineQueryRequest request = new TimelineQueryRequest();
        request.setView(TimelineView.WEEK);

        var response = timelineService.getTimeline(request);

        assertThat(response.getFromDate()).isEqualTo(LocalDate.of(2026, 9, 14));
        assertThat(response.getToDate()).isEqualTo(LocalDate.of(2026, 9, 20));
    }

    /**
     * 验证同时传了起止日期和视图时，以显式日期范围为准。
     */
    @Test
    void shouldPreferExplicitDateRangeOverView() {
        when(timelineMapper.selectTaskEvents(
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 10)))
                .thenReturn(List.of());
        when(timelineMapper.selectProjectEvents(
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 10)))
                .thenReturn(List.of());

        TimelineQueryRequest request = request(
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 10));
        request.setView(TimelineView.MONTH);
        request.setDate(LocalDate.of(2026, 9, 16));

        var response = timelineService.getTimeline(request);

        assertThat(response.getFromDate()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(response.getToDate()).isEqualTo(LocalDate.of(2026, 9, 10));
        verify(timelineMapper).selectTaskEvents(
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 10));
        verify(timelineMapper).selectProjectEvents(
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 10));
    }

    /**
     * 验证既没有日期范围也没有视图时返回校验错误。
     */
    @Test
    void shouldRejectMissingRangeAndView() {
        assertThatThrownBy(() -> timelineService.getTimeline(new TimelineQueryRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请提供日期范围或视图类型");
        verifyNoMoreInteractions(timelineMapper);
    }

    /**
     * 创建时间线查询参数。
     */
    private TimelineQueryRequest request(LocalDate fromDate, LocalDate toDate) {
        TimelineQueryRequest request = new TimelineQueryRequest();
        request.setFromDate(fromDate);
        request.setToDate(toDate);
        return request;
    }

    /**
     * 创建测试用任务事件。
     */
    private TimelineEventResponse event(Long id, String title, LocalDate date, TaskStatus status) {
        TimelineEventResponse event = new TimelineEventResponse();
        event.setType(TimelineEventType.TASK);
        event.setId(id);
        event.setTitle(title);
        event.setDate(date);
        event.setProjectId(10L);
        event.setProjectName("研发罗盘");
        event.setStatus(status);
        event.setPriority(TaskPriority.MEDIUM);
        return event;
    }

    /**
     * 创建测试用项目事件。
     */
    private TimelineEventResponse projectEvent(Long id, String title, LocalDate date,
            boolean completed) {
        TimelineEventResponse event = new TimelineEventResponse();
        event.setType(TimelineEventType.PROJECT);
        event.setId(id);
        event.setTitle(title);
        event.setDate(date);
        event.setProjectId(id);
        event.setProjectName(title);
        event.setCompleted(completed);
        return event;
    }
}
