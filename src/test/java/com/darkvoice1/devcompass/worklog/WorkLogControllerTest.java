package com.darkvoice1.devcompass.worklog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.darkvoice1.devcompass.common.exception.GlobalExceptionHandler;
import com.darkvoice1.devcompass.worklog.controller.WorkLogController;
import com.darkvoice1.devcompass.worklog.dto.WorkLogResponse;
import com.darkvoice1.devcompass.worklog.service.WorkLogService;

/**
 * 验证工作日志接口和参数校验。
 */
class WorkLogControllerTest {

    private WorkLogService workLogService;
    private MockMvc mockMvc;

    /**
     * 初始化带参数校验的 MockMvc。
     */
    @BeforeEach
    void setUp() {
        workLogService = mock(WorkLogService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new WorkLogController(workLogService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    /**
     * 验证可以按任务查询工作日志。
     */
    @Test
    void shouldGetWorkLogByTaskId() throws Exception {
        when(workLogService.getWorkLogByTaskId(10L)).thenReturn(response());

        mockMvc.perform(get("/api/v1/work-logs/tasks/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(10))
                .andExpect(jsonPath("$.data.summaryContent").value("完成接口开发"));
    }

    /**
     * 验证可以按日志日期范围查询工作日志。
     */
    @Test
    void shouldQueryWorkLogsByDateRange() throws Exception {
        when(workLogService.queryWorkLogsByDateRange(any())).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/work-logs")
                        .param("logDateFrom", "2026-09-01")
                        .param("logDateTo", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(20));
    }

    /**
     * 验证日期范围查询必须提供结束日期。
     */
    @Test
    void shouldRejectDateRangeWithoutEndDate() throws Exception {
        mockMvc.perform(get("/api/v1/work-logs").param("logDateFrom", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.logDateTo").value("日志结束日期不能为空"));
    }

    /**
     * 验证可以编辑工作日志。
     */
    @Test
    void shouldUpdateWorkLog() throws Exception {
        when(workLogService.updateWorkLog(any(), any())).thenReturn(response());

        mockMvc.perform(put("/api/v1/work-logs/20")
                        .contentType("application/json")
                        .content("{\"logDate\":\"2026-09-16\",\"summaryContent\":\"完成接口开发\",\"spentMinutes\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(20));
    }

    /**
     * 验证编辑日志时必须提供完成总结。
     */
    @Test
    void shouldRejectUpdateWithoutSummaryContent() throws Exception {
        mockMvc.perform(put("/api/v1/work-logs/20")
                        .contentType("application/json")
                        .content("{\"logDate\":\"2026-09-16\",\"spentMinutes\":90}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.summaryContent").value("完成总结不能为空"));
    }

    /**
     * 创建测试用工作日志响应。
     */
    private WorkLogResponse response() {
        WorkLogResponse response = new WorkLogResponse();
        response.setId(20L);
        response.setTaskId(10L);
        response.setLogDate(LocalDate.of(2026, 9, 16));
        response.setSummaryContent("完成接口开发");
        response.setSpentMinutes(90);
        return response;
    }
}
