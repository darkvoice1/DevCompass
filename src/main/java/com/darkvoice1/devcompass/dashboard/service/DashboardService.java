package com.darkvoice1.devcompass.dashboard.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.darkvoice1.devcompass.dashboard.dto.DashboardOverviewResponse;
import com.darkvoice1.devcompass.dashboard.dto.DashboardProjectQueryRequest;
import com.darkvoice1.devcompass.dashboard.dto.DashboardProjectResponse;
import com.darkvoice1.devcompass.dashboard.dto.DashboardProjectRow;
import com.darkvoice1.devcompass.dashboard.dto.ProjectHealthStatus;
import com.darkvoice1.devcompass.project.entity.ProgressMode;
import com.darkvoice1.devcompass.project.entity.ProjectStatus;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;

/**
 * 聚合多项目仪表盘所需的摘要数据。
 */
@Service
public class DashboardService {

    /**
     * 即将到期窗口：从今天起共 7 天，包含今天和第 7 天。
     */
    public static final int DUE_SOON_DAYS = 7;

    private final ProjectMapper projectMapper;

    /**
     * 创建仪表盘服务。
     *
     * @param projectMapper 项目数据访问对象
     */
    public DashboardService(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    /**
     * 查询未归档项目的数量、状态分布、健康度和项目摘要。
     *
     * @param request 项目筛选参数
     * @return 仪表盘聚合结果
     */
    public DashboardOverviewResponse getProjectOverview(DashboardProjectQueryRequest request) {
        List<DashboardProjectRow> projects = projectMapper.selectDashboardProjects(
                request.getStatus(), normalizeTag(request.getTag()), request.getActiveWithinDays(),
                DUE_SOON_DAYS);
        Map<ProjectStatus, Long> statusDistribution = createEmptyStatusDistribution();
        Map<ProjectHealthStatus, Long> healthDistribution = createEmptyHealthDistribution();
        List<DashboardProjectResponse> projectResponses = new ArrayList<>(projects.size());
        for (DashboardProjectRow project : projects) {
            ProjectStatus status = project.getStatus();
            Long currentCount = statusDistribution.get(status);
            statusDistribution.put(status, currentCount == null ? 1L : currentCount + 1L);
            DashboardProjectResponse projectResponse = toResponse(project);
            ProjectHealthStatus healthStatus = projectResponse.getHealthStatus();
            Long currentHealthCount = healthDistribution.get(healthStatus);
            healthDistribution.put(healthStatus,
                    currentHealthCount == null ? 1L : currentHealthCount + 1L);
            projectResponses.add(projectResponse);
        }

        DashboardOverviewResponse response = new DashboardOverviewResponse();
        response.setTotalProjects(projects.size());
        response.setStatusDistribution(statusDistribution);
        response.setHealthDistribution(healthDistribution);
        response.setProjects(projectResponses);
        return response;
    }

    /**
     * 创建包含全部项目状态的零值统计。
     */
    private Map<ProjectStatus, Long> createEmptyStatusDistribution() {
        Map<ProjectStatus, Long> distribution = new EnumMap<>(ProjectStatus.class);
        Arrays.stream(ProjectStatus.values()).forEach(status -> distribution.put(status, 0L));
        return distribution;
    }

    /**
     * 创建包含全部健康度的零值统计。
     */
    private Map<ProjectHealthStatus, Long> createEmptyHealthDistribution() {
        Map<ProjectHealthStatus, Long> distribution = new EnumMap<>(ProjectHealthStatus.class);
        Arrays.stream(ProjectHealthStatus.values())
                .forEach(healthStatus -> distribution.put(healthStatus, 0L));
        return distribution;
    }

    /**
     * 清理可选标签两侧的空白，空白标签按未筛选处理。
     */
    private String normalizeTag(String tag) {
        return tag == null || tag.isBlank() ? null : tag.trim();
    }

    /**
     * 将查询结果转换为仪表盘摘要，并计算项目健康度。
     */
    private DashboardProjectResponse toResponse(DashboardProjectRow project) {
        DashboardProjectResponse response = new DashboardProjectResponse();
        response.setId(project.getId());
        response.setName(project.getName());
        response.setStatus(project.getStatus());
        response.setProgress(project.getProgressMode() == ProgressMode.MANUAL
                ? project.getManualProgress() : project.getAutoProgress());
        response.setTags(project.getTags());
        response.setOverdueTaskCount(project.getOverdueTaskCount());
        response.setDueSoonTaskCount(project.getDueSoonTaskCount());
        response.setHealthStatus(resolveHealth(project));
        response.setUpdatedAt(project.getUpdatedAt());
        return response;
    }

    /**
     * 按逾期优先、其次预警的规则计算健康度。
     */
    private ProjectHealthStatus resolveHealth(DashboardProjectRow project) {
        if (project.getOverdueTaskCount() > 0 || isProjectTargetOverdue(project)) {
            return ProjectHealthStatus.OVERDUE;
        }
        if (project.getDueSoonTaskCount() > 0 || isProjectTargetDueSoon(project)) {
            return ProjectHealthStatus.AT_RISK;
        }
        return ProjectHealthStatus.HEALTHY;
    }

    /**
     * 未完成项目的目标日期早于今天时视为项目延期。
     */
    private boolean isProjectTargetOverdue(DashboardProjectRow project) {
        return project.getTargetDate() != null
                && project.getStatus() != ProjectStatus.COMPLETED
                && project.getTargetDate().isBefore(LocalDate.now());
    }

    /**
     * 未完成项目的目标日期落在即将到期窗口内时视为预警。
     */
    private boolean isProjectTargetDueSoon(DashboardProjectRow project) {
        if (project.getTargetDate() == null || project.getStatus() == ProjectStatus.COMPLETED) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate dueSoonUntil = today.plusDays(DUE_SOON_DAYS);
        LocalDate targetDate = project.getTargetDate();
        return !targetDate.isBefore(today) && !targetDate.isAfter(dueSoonUntil);
    }
}
