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

import com.darkvoice1.devcompass.dashboard.dto.FocusListItemResponse;
import com.darkvoice1.devcompass.dashboard.dto.FocusListQueryRequest;
import com.darkvoice1.devcompass.dashboard.dto.FocusListType;
import com.darkvoice1.devcompass.dashboard.service.FocusListService;
import com.darkvoice1.devcompass.task.entity.TaskStatus;
import com.darkvoice1.devcompass.task.repository.TaskMapper;

/**
 * 验证本周任务清单的日期边界和查询参数。
 */
class FocusListServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private TaskMapper taskMapper;

    private FocusListService focusListService;

    /**
     * 使用固定在周三的时钟，便于核对周一到周日的范围。
     */
    @BeforeEach
    void setUp() {
        taskMapper = mock(TaskMapper.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-16T08:00:00+08:00"), ZONE);
        focusListService = new FocusListService(taskMapper, clock);
    }

    /**
     * 验证本周清单使用应用时区的周一到周日，并包含今天。
     */
    @Test
    void shouldQueryThisWeekTasksWithMondayToSundayRange() {
        FocusListItemResponse item = item(1L, "实现本周清单", LocalDate.of(2026, 9, 16));
        when(taskMapper.selectFocusListTasks(
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20)))
                .thenReturn(List.of(item));

        var response = focusListService.getFocusList(request());

        assertThat(response.getType()).isEqualTo(FocusListType.THIS_WEEK);
        assertThat(response.getFromDate()).isEqualTo(LocalDate.of(2026, 9, 14));
        assertThat(response.getToDate()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getTaskId()).isEqualTo(1L);
        assertThat(response.getItems().get(0).getProjectId()).isEqualTo(10L);
        assertThat(response.getItems().get(0).getProjectName()).isEqualTo("研发罗盘");
        verify(taskMapper).selectFocusListTasks(
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20));
        verifyNoMoreInteractions(taskMapper);
    }

    /**
     * 验证没有本周任务时仍返回日期范围和空列表。
     */
    @Test
    void shouldReturnEmptyItemsWhenNoThisWeekTaskExists() {
        when(taskMapper.selectFocusListTasks(
                LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20)))
                .thenReturn(List.of());

        var response = focusListService.getFocusList(request());

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getFromDate()).isEqualTo(LocalDate.of(2026, 9, 14));
        assertThat(response.getToDate()).isEqualTo(LocalDate.of(2026, 9, 20));
    }

    /**
     * 创建本周清单查询参数。
     */
    private FocusListQueryRequest request() {
        FocusListQueryRequest request = new FocusListQueryRequest();
        request.setType(FocusListType.THIS_WEEK);
        return request;
    }

    /**
     * 创建测试用清单项。
     */
    private FocusListItemResponse item(Long taskId, String title, LocalDate dueDate) {
        FocusListItemResponse item = new FocusListItemResponse();
        item.setTaskId(taskId);
        item.setTitle(title);
        item.setStatus(TaskStatus.TODO);
        item.setDueDate(dueDate);
        item.setProjectId(10L);
        item.setProjectName("研发罗盘");
        return item;
    }
}
