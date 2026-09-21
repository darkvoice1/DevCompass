package com.darkvoice1.devcompass.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.darkvoice1.devcompass.activity.controller.ActivityController;
import com.darkvoice1.devcompass.activity.dto.ActivityPageResponse;
import com.darkvoice1.devcompass.activity.dto.ActivityQueryRequest;
import com.darkvoice1.devcompass.activity.dto.ActivityResponse;
import com.darkvoice1.devcompass.activity.entity.ActivityAction;
import com.darkvoice1.devcompass.activity.entity.ActivityObjectType;
import com.darkvoice1.devcompass.activity.service.ActivityService;
import com.darkvoice1.devcompass.common.exception.GlobalExceptionHandler;

/**
 * 验证动态查询接口和参数校验。
 */
class ActivityControllerTest {

    private ActivityService activityService;

    private MockMvc mockMvc;

    /**
     * 初始化带参数校验的 MockMvc。
     */
    @BeforeEach
    void setUp() {
        activityService = mock(ActivityService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ActivityController(activityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    /**
     * 验证按项目查询动态返回分页数据和摘要。
     */
    @Test
    void shouldQueryActivities() throws Exception {
        when(activityService.queryActivities(any())).thenReturn(pageResponse());

        mockMvc.perform(get("/api/v1/activities")
                        .param("projectId", "8")
                        .param("objectType", "TASK")
                        .param("dateFrom", "2026-09-01")
                        .param("dateTo", "2026-09-21")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].objectType").value("TASK"))
                .andExpect(jsonPath("$.data.records[0].action").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$.data.records[0].summary")
                        .value("任务状态从 TODO 变为 COMPLETED"));

        ArgumentCaptor<ActivityQueryRequest> captor = ArgumentCaptor.forClass(ActivityQueryRequest.class);
        verify(activityService).queryActivities(captor.capture());
        assertThat(captor.getValue().getProjectId()).isEqualTo(8L);
        assertThat(captor.getValue().getObjectType()).isEqualTo(ActivityObjectType.TASK);
        assertThat(captor.getValue().getDateFrom()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(captor.getValue().getDateTo()).isEqualTo(LocalDate.of(2026, 9, 21));
        assertThat(captor.getValue().getPage()).isEqualTo(1L);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    /**
     * 验证缺少项目时返回参数校验错误。
     */
    @Test
    void shouldRejectMissingProjectId() throws Exception {
        mockMvc.perform(get("/api/v1/activities"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.projectId").value("项目不能为空"));
    }

    /**
     * 验证每页数量超过上限时返回参数校验错误。
     */
    @Test
    void shouldRejectOversizedPageSize() throws Exception {
        mockMvc.perform(get("/api/v1/activities")
                        .param("projectId", "8")
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.pageSize").value("每页数量不能超过100"));
    }

    /**
     * 创建测试用分页响应。
     */
    private ActivityPageResponse pageResponse() {
        ActivityResponse item = new ActivityResponse();
        item.setId(21L);
        item.setProjectId(8L);
        item.setObjectType(ActivityObjectType.TASK);
        item.setObjectId(3L);
        item.setAction(ActivityAction.STATUS_CHANGED);
        item.setSummary("任务状态从 TODO 变为 COMPLETED");
        item.setCreatedAt(Instant.parse("2026-09-21T02:00:00Z"));

        ActivityPageResponse response = new ActivityPageResponse();
        response.setRecords(List.of(item));
        response.setTotal(1);
        response.setPage(1);
        response.setPageSize(20);
        response.setTotalPages(1);
        return response;
    }
}
