package com.darkvoice1.devcompass.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.darkvoice1.devcompass.dashboard.dto.DashboardProjectQueryRequest;
import com.darkvoice1.devcompass.dashboard.dto.DashboardProjectRow;
import com.darkvoice1.devcompass.dashboard.dto.ProjectHealthStatus;
import com.darkvoice1.devcompass.dashboard.service.DashboardService;
import com.darkvoice1.devcompass.project.entity.ProgressMode;
import com.darkvoice1.devcompass.project.entity.ProjectStatus;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;

/**
 * 验证多项目仪表盘聚合业务。
 */
class DashboardServiceTest {

    private ProjectMapper projectMapper;

    private DashboardService dashboardService;

    /**
     * 初始化仪表盘服务及 Mapper 模拟对象。
     */
    @BeforeEach
    void setUp() {
        projectMapper = mock(ProjectMapper.class);
        dashboardService = new DashboardService(projectMapper);
    }

    /**
     * 验证一次查询可生成项目总数、完整状态分布和项目摘要。
     */
    @Test
    void shouldBuildProjectOverviewWithSingleQuery() {
        DashboardProjectRow automaticProject = project(1L, "研发罗盘", ProjectStatus.IN_PROGRESS,
                ProgressMode.AUTO, 40, null, Instant.parse("2026-09-17T08:00:00Z"));
        DashboardProjectRow manualProject = project(2L, "个人博客", ProjectStatus.PAUSED,
                ProgressMode.MANUAL, 80, 60, Instant.parse("2026-09-16T08:00:00Z"));
        when(projectMapper.selectDashboardProjects(null, null, null, DashboardService.DUE_SOON_DAYS))
                .thenReturn(List.of(automaticProject, manualProject));

        var response = dashboardService.getProjectOverview(new DashboardProjectQueryRequest());

        assertThat(response.getTotalProjects()).isEqualTo(2);
        assertThat(response.getStatusDistribution())
                .containsEntry(ProjectStatus.PLANNED, 0L)
                .containsEntry(ProjectStatus.IN_PROGRESS, 1L)
                .containsEntry(ProjectStatus.COMPLETED, 0L)
                .containsEntry(ProjectStatus.PAUSED, 1L);
        assertThat(response.getHealthDistribution())
                .containsEntry(ProjectHealthStatus.HEALTHY, 2L)
                .containsEntry(ProjectHealthStatus.AT_RISK, 0L)
                .containsEntry(ProjectHealthStatus.OVERDUE, 0L);
        assertThat(response.getProjects()).extracting(project -> project.getName())
                .containsExactly("研发罗盘", "个人博客");
        assertThat(response.getProjects()).extracting(project -> project.getProgress())
                .containsExactly(40, 60);
        assertThat(response.getProjects()).extracting(project -> project.getHealthStatus())
                .containsExactly(ProjectHealthStatus.HEALTHY, ProjectHealthStatus.HEALTHY);
        verify(projectMapper).selectDashboardProjects(null, null, null, DashboardService.DUE_SOON_DAYS);
        verifyNoMoreInteractions(projectMapper);
    }

    /**
     * 验证没有项目时仍返回全部状态和健康度的零值统计。
     */
    @Test
    void shouldReturnEmptyOverviewWhenNoProjectExists() {
        when(projectMapper.selectDashboardProjects(null, null, null, DashboardService.DUE_SOON_DAYS))
                .thenReturn(List.of());

        var response = dashboardService.getProjectOverview(new DashboardProjectQueryRequest());

        assertThat(response.getTotalProjects()).isZero();
        assertThat(response.getProjects()).isEmpty();
        assertThat(response.getStatusDistribution()).hasSize(ProjectStatus.values().length)
                .allSatisfy((status, count) -> assertThat(count).isZero());
        assertThat(response.getHealthDistribution()).hasSize(ProjectHealthStatus.values().length)
                .allSatisfy((healthStatus, count) -> assertThat(count).isZero());
    }

    /**
     * 验证筛选条件会传给单次项目查询，并清理标签两侧空白。
     */
    @Test
    void shouldPassNormalizedFiltersToProjectQuery() {
        DashboardProjectQueryRequest request = new DashboardProjectQueryRequest();
        request.setStatus(ProjectStatus.IN_PROGRESS);
        request.setTag("  后端  ");
        request.setActiveWithinDays(30);
        when(projectMapper.selectDashboardProjects(
                ProjectStatus.IN_PROGRESS, "后端", 30, DashboardService.DUE_SOON_DAYS))
                .thenReturn(List.of());

        dashboardService.getProjectOverview(request);

        verify(projectMapper).selectDashboardProjects(
                ProjectStatus.IN_PROGRESS, "后端", 30, DashboardService.DUE_SOON_DAYS);
        verifyNoMoreInteractions(projectMapper);
    }

    /**
     * 验证有逾期任务时项目健康度为延期。
     */
    @Test
    void shouldMarkProjectOverdueWhenItHasOverdueTasks() {
        DashboardProjectRow project = project(1L, "研发罗盘", ProjectStatus.IN_PROGRESS,
                ProgressMode.AUTO, 40, null, Instant.parse("2026-09-17T08:00:00Z"));
        project.setOverdueTaskCount(2);
        project.setDueSoonTaskCount(1);
        when(projectMapper.selectDashboardProjects(null, null, null, DashboardService.DUE_SOON_DAYS))
                .thenReturn(List.of(project));

        var response = dashboardService.getProjectOverview(new DashboardProjectQueryRequest());

        assertThat(response.getProjects().get(0).getHealthStatus()).isEqualTo(ProjectHealthStatus.OVERDUE);
        assertThat(response.getProjects().get(0).getOverdueTaskCount()).isEqualTo(2);
        assertThat(response.getProjects().get(0).getDueSoonTaskCount()).isEqualTo(1);
        assertThat(response.getHealthDistribution()).containsEntry(ProjectHealthStatus.OVERDUE, 1L);
    }

    /**
     * 验证只有即将到期任务时项目健康度为预警。
     */
    @Test
    void shouldMarkProjectAtRiskWhenTaskIsDueSoon() {
        DashboardProjectRow project = project(1L, "研发罗盘", ProjectStatus.IN_PROGRESS,
                ProgressMode.AUTO, 40, null, Instant.parse("2026-09-17T08:00:00Z"));
        project.setDueSoonTaskCount(3);
        when(projectMapper.selectDashboardProjects(null, null, null, DashboardService.DUE_SOON_DAYS))
                .thenReturn(List.of(project));

        var response = dashboardService.getProjectOverview(new DashboardProjectQueryRequest());

        assertThat(response.getProjects().get(0).getHealthStatus()).isEqualTo(ProjectHealthStatus.AT_RISK);
        assertThat(response.getHealthDistribution()).containsEntry(ProjectHealthStatus.AT_RISK, 1L);
    }

    /**
     * 验证未完成项目的目标日期已过时健康度为延期。
     */
    @Test
    void shouldMarkProjectOverdueWhenTargetDatePassed() {
        DashboardProjectRow project = project(1L, "研发罗盘", ProjectStatus.IN_PROGRESS,
                ProgressMode.AUTO, 40, null, Instant.parse("2026-09-17T08:00:00Z"));
        project.setTargetDate(LocalDate.now().minusDays(1));
        when(projectMapper.selectDashboardProjects(null, null, null, DashboardService.DUE_SOON_DAYS))
                .thenReturn(List.of(project));

        var response = dashboardService.getProjectOverview(new DashboardProjectQueryRequest());

        assertThat(response.getProjects().get(0).getHealthStatus()).isEqualTo(ProjectHealthStatus.OVERDUE);
    }

    /**
     * 验证未完成项目的目标日期落在窗口内时健康度为预警。
     */
    @Test
    void shouldMarkProjectAtRiskWhenTargetDateIsDueSoon() {
        DashboardProjectRow project = project(1L, "研发罗盘", ProjectStatus.IN_PROGRESS,
                ProgressMode.AUTO, 40, null, Instant.parse("2026-09-17T08:00:00Z"));
        project.setTargetDate(LocalDate.now().plusDays(DashboardService.DUE_SOON_DAYS));
        when(projectMapper.selectDashboardProjects(null, null, null, DashboardService.DUE_SOON_DAYS))
                .thenReturn(List.of(project));

        var response = dashboardService.getProjectOverview(new DashboardProjectQueryRequest());

        assertThat(response.getProjects().get(0).getHealthStatus()).isEqualTo(ProjectHealthStatus.AT_RISK);
    }

    /**
     * 验证已完成项目即使目标日期已过，只要没有逾期任务仍视为健康。
     */
    @Test
    void shouldKeepCompletedProjectHealthyWhenTargetDatePassed() {
        DashboardProjectRow project = project(1L, "研发罗盘", ProjectStatus.COMPLETED,
                ProgressMode.AUTO, 100, null, Instant.parse("2026-09-17T08:00:00Z"));
        project.setTargetDate(LocalDate.now().minusDays(10));
        when(projectMapper.selectDashboardProjects(null, null, null, DashboardService.DUE_SOON_DAYS))
                .thenReturn(List.of(project));

        var response = dashboardService.getProjectOverview(new DashboardProjectQueryRequest());

        assertThat(response.getProjects().get(0).getHealthStatus()).isEqualTo(ProjectHealthStatus.HEALTHY);
        assertThat(response.getHealthDistribution()).containsEntry(ProjectHealthStatus.HEALTHY, 1L);
    }

    /**
     * 创建测试用仪表盘查询结果。
     */
    private DashboardProjectRow project(Long id, String name, ProjectStatus status,
            ProgressMode progressMode, Integer autoProgress, Integer manualProgress, Instant updatedAt) {
        DashboardProjectRow project = new DashboardProjectRow();
        project.setId(id);
        project.setName(name);
        project.setStatus(status);
        project.setProgressMode(progressMode);
        project.setAutoProgress(autoProgress);
        project.setManualProgress(manualProgress);
        project.setTags("后端,学习项目");
        project.setUpdatedAt(updatedAt);
        return project;
    }
}
