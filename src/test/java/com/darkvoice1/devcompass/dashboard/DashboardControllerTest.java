package com.darkvoice1.devcompass.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.darkvoice1.devcompass.common.exception.GlobalExceptionHandler;
import com.darkvoice1.devcompass.dashboard.controller.DashboardController;
import com.darkvoice1.devcompass.dashboard.dto.DashboardOverviewResponse;
import com.darkvoice1.devcompass.dashboard.dto.DashboardProjectQueryRequest;
import com.darkvoice1.devcompass.dashboard.dto.DashboardProjectResponse;
import com.darkvoice1.devcompass.dashboard.dto.ProjectHealthStatus;
import com.darkvoice1.devcompass.dashboard.service.DashboardService;
import com.darkvoice1.devcompass.project.entity.ProjectStatus;

/**
 * 验证多项目仪表盘查询接口。
 */
class DashboardControllerTest {

    private DashboardService dashboardService;

    private MockMvc mockMvc;

    /**
     * 初始化仪表盘控制器测试环境。
     */
    @BeforeEach
    void setUp() {
        dashboardService = mock(DashboardService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new DashboardController(dashboardService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    /**
     * 验证接口返回项目总数、状态分布和项目摘要。
     */
    @Test
    void shouldGetProjectOverview() throws Exception {
        when(dashboardService.getProjectOverview(any())).thenReturn(overviewResponse());

        mockMvc.perform(get("/api/v1/dashboard/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.totalProjects").value(1))
                .andExpect(jsonPath("$.data.statusDistribution.PLANNED").value(0))
                .andExpect(jsonPath("$.data.statusDistribution.IN_PROGRESS").value(1))
                .andExpect(jsonPath("$.data.projects[0].id").value(1))
                .andExpect(jsonPath("$.data.projects[0].name").value("研发罗盘"))
                .andExpect(jsonPath("$.data.projects[0].progress").value(40))
                .andExpect(jsonPath("$.data.projects[0].healthStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.data.projects[0].overdueTaskCount").value(0))
                .andExpect(jsonPath("$.data.projects[0].dueSoonTaskCount").value(0))
                .andExpect(jsonPath("$.data.healthDistribution.HEALTHY").value(1))
                .andExpect(jsonPath("$.data.healthDistribution.AT_RISK").value(0))
                .andExpect(jsonPath("$.data.healthDistribution.OVERDUE").value(0))
                .andExpect(jsonPath("$.data.projects[0].updatedAt")
                        .value("2026-09-17T08:00:00Z"));
    }

    /**
     * 验证状态、标签和最近活跃天数可以绑定为筛选参数。
     */
    @Test
    void shouldBindProjectFilters() throws Exception {
        when(dashboardService.getProjectOverview(any())).thenReturn(overviewResponse());

        mockMvc.perform(get("/api/v1/dashboard/projects")
                        .param("status", "IN_PROGRESS")
                        .param("tag", "后端")
                        .param("activeWithinDays", "30"))
                .andExpect(status().isOk());

        ArgumentCaptor<DashboardProjectQueryRequest> captor =
                ArgumentCaptor.forClass(DashboardProjectQueryRequest.class);
        verify(dashboardService).getProjectOverview(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(captor.getValue().getTag()).isEqualTo("后端");
        assertThat(captor.getValue().getActiveWithinDays()).isEqualTo(30);
    }

    /**
     * 验证最近活跃天数不能小于一天。
     */
    @Test
    void shouldRejectInvalidActiveWithinDays() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/projects")
                        .param("activeWithinDays", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.activeWithinDays")
                        .value("最近活跃天数必须大于等于1"));
    }

    /**
     * 创建测试用仪表盘响应。
     */
    private DashboardOverviewResponse overviewResponse() {
        DashboardProjectResponse project = new DashboardProjectResponse();
        project.setId(1L);
        project.setName("研发罗盘");
        project.setStatus(ProjectStatus.IN_PROGRESS);
        project.setProgress(40);
        project.setTags("后端,学习项目");
        project.setHealthStatus(ProjectHealthStatus.HEALTHY);
        project.setUpdatedAt(Instant.parse("2026-09-17T08:00:00Z"));

        DashboardOverviewResponse response = new DashboardOverviewResponse();
        response.setTotalProjects(1);
        response.setStatusDistribution(Map.of(
                ProjectStatus.PLANNED, 0L,
                ProjectStatus.IN_PROGRESS, 1L,
                ProjectStatus.COMPLETED, 0L,
                ProjectStatus.PAUSED, 0L));
        response.setHealthDistribution(Map.of(
                ProjectHealthStatus.HEALTHY, 1L,
                ProjectHealthStatus.AT_RISK, 0L,
                ProjectHealthStatus.OVERDUE, 0L));
        response.setProjects(List.of(project));
        return response;
    }
}
