package com.darkvoice1.devcompass.dashboard.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.darkvoice1.devcompass.dashboard.dto.DashboardOverviewResponse;
import com.darkvoice1.devcompass.dashboard.dto.DashboardProjectResponse;
import com.darkvoice1.devcompass.project.entity.ProgressMode;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.entity.ProjectStatus;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;

/**
 * 聚合多项目仪表盘所需的摘要数据。
 */
@Service
public class DashboardService {

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
     * 查询未归档项目的数量、状态分布和项目摘要。
     *
     * @return 仪表盘聚合结果
     */
    public DashboardOverviewResponse getProjectOverview() {
        List<Project> projects = projectMapper.selectDashboardProjects();
        Map<ProjectStatus, Long> statusDistribution = createEmptyStatusDistribution();
        List<DashboardProjectResponse> projectResponses = new ArrayList<>(projects.size());
        for (Project project : projects) {
            ProjectStatus status = project.getStatus();
            Long currentCount = statusDistribution.get(status);
            statusDistribution.put(status, currentCount == null ? 1L : currentCount + 1L);
            projectResponses.add(toResponse(project));
        }

        DashboardOverviewResponse response = new DashboardOverviewResponse();
        response.setTotalProjects(projects.size());
        response.setStatusDistribution(statusDistribution);
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
     * 将项目实体转换为仪表盘摘要。
     */
    private DashboardProjectResponse toResponse(Project project) {
        DashboardProjectResponse response = new DashboardProjectResponse();
        response.setId(project.getId());
        response.setName(project.getName());
        response.setStatus(project.getStatus());
        response.setProgress(project.getProgressMode() == ProgressMode.MANUAL
                ? project.getManualProgress() : project.getAutoProgress());
        response.setTags(project.getTags());
        response.setUpdatedAt(project.getUpdatedAt());
        return response;
    }
}
