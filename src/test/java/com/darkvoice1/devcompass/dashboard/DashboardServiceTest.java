package com.darkvoice1.devcompass.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.darkvoice1.devcompass.dashboard.dto.DashboardProjectQueryRequest;
import com.darkvoice1.devcompass.dashboard.service.DashboardService;
import com.darkvoice1.devcompass.project.entity.ProgressMode;
import com.darkvoice1.devcompass.project.entity.Project;
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
        Project automaticProject = project(1L, "研发罗盘", ProjectStatus.IN_PROGRESS,
                ProgressMode.AUTO, 40, null, Instant.parse("2026-09-17T08:00:00Z"));
        Project manualProject = project(2L, "个人博客", ProjectStatus.PAUSED,
                ProgressMode.MANUAL, 80, 60, Instant.parse("2026-09-16T08:00:00Z"));
        when(projectMapper.selectDashboardProjects(null, null, null))
                .thenReturn(List.of(automaticProject, manualProject));

        var response = dashboardService.getProjectOverview(new DashboardProjectQueryRequest());

        assertThat(response.getTotalProjects()).isEqualTo(2);
        assertThat(response.getStatusDistribution())
                .containsEntry(ProjectStatus.PLANNED, 0L)
                .containsEntry(ProjectStatus.IN_PROGRESS, 1L)
                .containsEntry(ProjectStatus.COMPLETED, 0L)
                .containsEntry(ProjectStatus.PAUSED, 1L);
        assertThat(response.getProjects()).extracting(project -> project.getName())
                .containsExactly("研发罗盘", "个人博客");
        assertThat(response.getProjects()).extracting(project -> project.getProgress())
                .containsExactly(40, 60);
        verify(projectMapper).selectDashboardProjects(null, null, null);
        verifyNoMoreInteractions(projectMapper);
    }

    /**
     * 验证没有项目时仍返回全部状态的零值统计。
     */
    @Test
    void shouldReturnEmptyOverviewWhenNoProjectExists() {
        when(projectMapper.selectDashboardProjects(null, null, null)).thenReturn(List.of());

        var response = dashboardService.getProjectOverview(new DashboardProjectQueryRequest());

        assertThat(response.getTotalProjects()).isZero();
        assertThat(response.getProjects()).isEmpty();
        assertThat(response.getStatusDistribution()).hasSize(ProjectStatus.values().length)
                .allSatisfy((status, count) -> assertThat(count).isZero());
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
        when(projectMapper.selectDashboardProjects(ProjectStatus.IN_PROGRESS, "后端", 30))
                .thenReturn(List.of());

        dashboardService.getProjectOverview(request);

        verify(projectMapper).selectDashboardProjects(ProjectStatus.IN_PROGRESS, "后端", 30);
        verifyNoMoreInteractions(projectMapper);
    }

    /**
     * 创建测试用项目。
     */
    private Project project(Long id, String name, ProjectStatus status, ProgressMode progressMode,
            Integer autoProgress, Integer manualProgress, Instant updatedAt) {
        Project project = new Project();
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
