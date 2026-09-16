package com.darkvoice1.devcompass.worklog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.task.entity.Task;
import com.darkvoice1.devcompass.task.repository.TaskMapper;
import com.darkvoice1.devcompass.worklog.dto.WorkLogContentRequest;
import com.darkvoice1.devcompass.worklog.dto.WorkLogDateRangeQueryRequest;
import com.darkvoice1.devcompass.worklog.entity.WorkLog;
import com.darkvoice1.devcompass.worklog.repository.WorkLogMapper;
import com.darkvoice1.devcompass.worklog.service.WorkLogService;

/**
 * 验证工作日志保存、查询和编辑业务。
 */
class WorkLogServiceTest {

    private WorkLogMapper workLogMapper;
    private TaskMapper taskMapper;
    private WorkLogService workLogService;

    /**
     * 初始化工作日志服务及 Mapper 模拟对象。
     */
    @BeforeEach
    void setUp() {
        workLogMapper = mock(WorkLogMapper.class);
        taskMapper = mock(TaskMapper.class);
        workLogService = new WorkLogService(workLogMapper, taskMapper);
    }

    /**
     * 验证首次完成任务时创建工作日志。
     */
    @Test
    void shouldCreateCompletionLog() {
        when(workLogMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            WorkLog workLog = invocation.getArgument(0);
            workLog.setId(20L);
            return 1;
        }).when(workLogMapper).insert(any(WorkLog.class));

        var response = workLogService.saveCompletionLog(10L, request());

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getTaskId()).isEqualTo(10L);
        assertThat(response.getSummaryContent()).isEqualTo("完成接口开发");
        assertThat(response.getCommitHashes()).isEqualTo("2dfd4ff,ff072e1");
        assertThat(response.getSpentMinutes()).isEqualTo(90);
    }

    /**
     * 验证重新完成任务时更新原有工作日志。
     */
    @Test
    void shouldUpdateExistingCompletionLog() {
        WorkLog existing = workLog(20L, 10L);
        when(workLogMapper.selectOne(any())).thenReturn(existing);
        WorkLogContentRequest request = request();
        request.setSummaryContent("修正后完成接口开发");

        var response = workLogService.saveCompletionLog(10L, request);

        assertThat(response.getSummaryContent()).isEqualTo("修正后完成接口开发");
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(workLogMapper).updateById(existing);
    }

    /**
     * 验证按任务查询不存在的日志时返回业务错误。
     */
    @Test
    void shouldRejectQueryingMissingWorkLog() {
        when(workLogMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> workLogService.getWorkLogByTaskId(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("任务暂无工作日志");
    }

    /**
     * 验证可按日期范围查询日志，并按最近日志优先排序。
     */
    @Test
    void shouldQueryWorkLogsByDateRange() {
        WorkLog latest = workLog(20L, 10L);
        latest.setLogDate(LocalDate.of(2026, 9, 16));
        WorkLog earlier = workLog(21L, 11L);
        earlier.setLogDate(LocalDate.of(2026, 9, 15));
        when(workLogMapper.selectList(any())).thenReturn(List.of(latest, earlier));

        WorkLogDateRangeQueryRequest request = dateRange(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        var responses = workLogService.queryWorkLogsByDateRange(request);

        assertThat(responses).extracting(response -> response.getId())
                .containsExactly(20L, 21L);
        ArgumentCaptor<QueryWrapper<WorkLog>> captor = ArgumentCaptor.captor();
        verify(workLogMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment())
                .contains("log_date", "ORDER BY log_date DESC", "updated_at DESC", "id DESC");
        assertThat(captor.getValue().getParamNameValuePairs().values())
                .contains(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
    }

    /**
     * 验证开始日期晚于结束日期时拒绝查询。
     */
    @Test
    void shouldRejectInvalidWorkLogDateRange() {
        WorkLogDateRangeQueryRequest request = dateRange(
                LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1));

        assertThatThrownBy(() -> workLogService.queryWorkLogsByDateRange(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("日志日期范围不合法");
    }

    /**
     * 验证可以按日期分组导出 Markdown 格式的工作日志。
     */
    @Test
    void shouldExportWorkLogsAsMarkdown() {
        WorkLog workLog = workLog(20L, 10L);
        workLog.setLogDate(LocalDate.of(2026, 9, 16));
        workLog.setPlanContent("完成日志导出");
        workLog.setSummaryContent("已完成 Markdown 导出接口");
        workLog.setCommitHashes("2dfd4ff,ff072e1");
        workLog.setSpentMinutes(90);
        workLog.setBlockerReason("暂无阻塞");
        when(workLogMapper.selectList(any())).thenReturn(List.of(workLog));
        Task task = new Task();
        task.setTitle("实现日志导出");
        when(taskMapper.selectById(10L)).thenReturn(task);

        String markdown = workLogService.exportWorkLogs(dateRange(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)));

        assertThat(markdown).contains(
                "# 工作日志",
                "## 2026-09-16",
                "### 任务 #10：实现日志导出",
                "- 完成总结：已完成 Markdown 导出接口",
                "- 提交记录：2dfd4ff,ff072e1",
                "- 实际耗时：90 分钟");
    }

    /**
     * 验证编辑日志会更新其内容和更新时间。
     */
    @Test
    void shouldUpdateWorkLog() {
        WorkLog existing = workLog(20L, 10L);
        when(workLogMapper.selectById(20L)).thenReturn(existing);
        WorkLogContentRequest request = request();
        request.setSpentMinutes(120);

        var response = workLogService.updateWorkLog(20L, request);

        assertThat(response.getSpentMinutes()).isEqualTo(120);
        assertThat(response.getUpdatedAt()).isNotNull();
        verify(workLogMapper).updateById(existing);
    }

    /**
     * 创建测试用日志内容。
     */
    private WorkLogContentRequest request() {
        WorkLogContentRequest request = new WorkLogContentRequest();
        request.setLogDate(LocalDate.of(2026, 9, 16));
        request.setPlanContent("完成接口");
        request.setSummaryContent("完成接口开发");
        request.setCommitHashes("2dfd4ff, ff072e1");
        request.setSpentMinutes(90);
        return request;
    }

    /**
     * 创建测试用工作日志实体。
     */
    private WorkLog workLog(Long id, Long taskId) {
        WorkLog workLog = new WorkLog();
        workLog.setId(id);
        workLog.setTaskId(taskId);
        return workLog;
    }

    /**
     * 创建测试用日期范围查询参数。
     */
    private WorkLogDateRangeQueryRequest dateRange(LocalDate from, LocalDate to) {
        WorkLogDateRangeQueryRequest request = new WorkLogDateRangeQueryRequest();
        request.setLogDateFrom(from);
        request.setLogDateTo(to);
        return request;
    }
}
