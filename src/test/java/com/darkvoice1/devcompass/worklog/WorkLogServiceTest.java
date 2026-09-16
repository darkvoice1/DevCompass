package com.darkvoice1.devcompass.worklog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.worklog.dto.WorkLogContentRequest;
import com.darkvoice1.devcompass.worklog.entity.WorkLog;
import com.darkvoice1.devcompass.worklog.repository.WorkLogMapper;
import com.darkvoice1.devcompass.worklog.service.WorkLogService;

/**
 * 验证工作日志保存、查询和编辑业务。
 */
class WorkLogServiceTest {

    private WorkLogMapper workLogMapper;
    private WorkLogService workLogService;

    /**
     * 初始化工作日志服务及 Mapper 模拟对象。
     */
    @BeforeEach
    void setUp() {
        workLogMapper = mock(WorkLogMapper.class);
        workLogService = new WorkLogService(workLogMapper);
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
}
