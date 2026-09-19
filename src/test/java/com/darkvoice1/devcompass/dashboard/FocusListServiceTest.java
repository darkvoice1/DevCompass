package com.darkvoice1.devcompass.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.darkvoice1.devcompass.dashboard.dto.FocusListItemKind;
import com.darkvoice1.devcompass.dashboard.dto.FocusListItemResponse;
import com.darkvoice1.devcompass.dashboard.dto.FocusListQueryRequest;
import com.darkvoice1.devcompass.dashboard.dto.FocusListType;
import com.darkvoice1.devcompass.dashboard.service.DashboardService;
import com.darkvoice1.devcompass.dashboard.service.FocusListService;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.task.entity.TaskStatus;
import com.darkvoice1.devcompass.task.repository.TaskMapper;

/**
 * 验证焦点清单的日期边界和查询分流。
 */
class FocusListServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private TaskMapper taskMapper;

    private ProjectMapper projectMapper;

    private FocusListService focusListService;

    /**
     * 使用固定在周三的时钟，便于核对各类日期范围。
     */
    @BeforeEach
    void setUp() {
        taskMapper = mock(TaskMapper.class);
        projectMapper = mock(ProjectMapper.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-16T08:00:00+08:00"), ZONE);
        focusListService = new FocusListService(taskMapper, projectMapper, clock);
    }

    /**
     * 验证本周清单使用应用时区的周一到周日，并包含今天。
     */
    @Test
    void shouldQueryThisWeekTasksWithMondayToSundayRange() {
        FocusListItemResponse item = taskItem(1L, "实现本周清单", LocalDate.of(2026, 9, 16));
        when(taskMapper.selectFocusListTasks(
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20)))
                .thenReturn(List.of(item));

        var response = focusListService.getFocusList(request(FocusListType.THIS_WEEK));

        assertThat(response.getType()).isEqualTo(FocusListType.THIS_WEEK);
        assertThat(response.getFromDate()).isEqualTo(LocalDate.of(2026, 9, 14));
        assertThat(response.getToDate()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getTaskId()).isEqualTo(1L);
        assertThat(response.getItems().get(0).getProjectId()).isEqualTo(10L);
        assertThat(response.getItems().get(0).getProjectName()).isEqualTo("研发罗盘");
        verify(taskMapper).selectFocusListTasks(
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20));
        verifyNoMoreInteractions(taskMapper, projectMapper);
    }

    /**
     * 验证没有本周任务时仍返回日期范围和空列表。
     */
    @Test
    void shouldReturnEmptyItemsWhenNoThisWeekTaskExists() {
        when(taskMapper.selectFocusListTasks(
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20)))
                .thenReturn(List.of());

        var response = focusListService.getFocusList(request(FocusListType.THIS_WEEK));

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getFromDate()).isEqualTo(LocalDate.of(2026, 9, 14));
        assertThat(response.getToDate()).isEqualTo(LocalDate.of(2026, 9, 20));
    }

    /**
     * 验证逾期清单包含昨天及更早的任务，以及目标日期已过的项目。
     */
    @Test
    void shouldQueryOverdueTasksAndDelayedProjects() {
        FocusListItemResponse overdueTask = taskItem(1L, "逾期任务", LocalDate.of(2026, 9, 15));
        FocusListItemResponse delayedProject = projectItem(20L, "延期项目", LocalDate.of(2026, 9, 1));
        when(taskMapper.selectFocusListTasks(null, LocalDate.of(2026, 9, 15)))
                .thenReturn(List.of(overdueTask));
        when(projectMapper.selectOverdueFocusProjects(LocalDate.of(2026, 9, 16)))
                .thenReturn(List.of(delayedProject));

        var response = focusListService.getFocusList(request(FocusListType.OVERDUE));

        assertThat(response.getType()).isEqualTo(FocusListType.OVERDUE);
        assertThat(response.getFromDate()).isNull();
        assertThat(response.getToDate()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(response.getItems()).extracting(item -> item.getItemKind())
                .containsExactly(FocusListItemKind.TASK, FocusListItemKind.PROJECT);
        assertThat(response.getItems()).extracting(item -> item.getTitle())
                .containsExactly("逾期任务", "延期项目");
        verify(taskMapper).selectFocusListTasks(null, LocalDate.of(2026, 9, 15));
        verify(projectMapper).selectOverdueFocusProjects(LocalDate.of(2026, 9, 16));
        verifyNoMoreInteractions(taskMapper, projectMapper);
    }

    /**
     * 验证即将到期清单查询今天到未来 7 天的任务，今天不算逾期。
     */
    @Test
    void shouldQueryDueSoonTasksFromTodayThroughSevenDays() {
        LocalDate today = LocalDate.of(2026, 9, 16);
        LocalDate toDate = today.plusDays(DashboardService.DUE_SOON_DAYS);
        FocusListItemResponse item = taskItem(1L, "即将到期任务", today);
        when(taskMapper.selectFocusListTasks(today, toDate)).thenReturn(List.of(item));

        var response = focusListService.getFocusList(request(FocusListType.DUE_SOON));

        assertThat(response.getType()).isEqualTo(FocusListType.DUE_SOON);
        assertThat(response.getFromDate()).isEqualTo(today);
        assertThat(response.getToDate()).isEqualTo(LocalDate.of(2026, 9, 23));
        assertThat(response.getItems()).extracting(itemResponse -> itemResponse.getTitle())
                .containsExactly("即将到期任务");
        verify(taskMapper).selectFocusListTasks(today, toDate);
        verifyNoMoreInteractions(taskMapper, projectMapper);
    }

    /**
     * 验证阻塞清单只查询阻塞任务，不按日期筛选。
     */
    @Test
    void shouldQueryBlockedTasksWithoutDateRange() {
        FocusListItemResponse item = taskItem(1L, "卡住的任务", LocalDate.of(2026, 9, 20));
        item.setBlockerReason("依赖登录接口");
        when(taskMapper.selectBlockedFocusListTasks()).thenReturn(List.of(item));

        var response = focusListService.getFocusList(request(FocusListType.BLOCKED));

        assertThat(response.getType()).isEqualTo(FocusListType.BLOCKED);
        assertThat(response.getFromDate()).isNull();
        assertThat(response.getToDate()).isNull();
        assertThat(response.getItems()).extracting(itemResponse -> itemResponse.getTitle())
                .containsExactly("卡住的任务");
        assertThat(response.getItems().get(0).getBlockerReason()).isEqualTo("依赖登录接口");
        verify(taskMapper).selectBlockedFocusListTasks();
        verifyNoMoreInteractions(taskMapper, projectMapper);
    }

    /**
     * 创建指定类型的清单查询参数。
     */
    private FocusListQueryRequest request(FocusListType type) {
        FocusListQueryRequest request = new FocusListQueryRequest();
        request.setType(type);
        return request;
    }

    /**
     * 创建测试用任务清单项。
     */
    private FocusListItemResponse taskItem(Long taskId, String title, LocalDate dueDate) {
        FocusListItemResponse item = new FocusListItemResponse();
        item.setItemKind(FocusListItemKind.TASK);
        item.setTaskId(taskId);
        item.setTitle(title);
        item.setStatus(TaskStatus.TODO);
        item.setDueDate(dueDate);
        item.setProjectId(10L);
        item.setProjectName("研发罗盘");
        return item;
    }

    /**
     * 创建测试用项目延期清单项。
     */
    private FocusListItemResponse projectItem(Long projectId, String title, LocalDate targetDate) {
        FocusListItemResponse item = new FocusListItemResponse();
        item.setItemKind(FocusListItemKind.PROJECT);
        item.setTitle(title);
        item.setDueDate(targetDate);
        item.setProjectId(projectId);
        item.setProjectName(title);
        return item;
    }
}
