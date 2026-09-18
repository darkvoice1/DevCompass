package com.darkvoice1.devcompass.dashboard.dto;

import java.util.List;
import java.util.Map;

import com.darkvoice1.devcompass.project.entity.ProjectStatus;

/**
 * 多项目仪表盘聚合结果。
 */
public class DashboardOverviewResponse {

    private long totalProjects;

    private Map<ProjectStatus, Long> statusDistribution;

    private Map<ProjectHealthStatus, Long> healthDistribution;

    private List<DashboardProjectResponse> projects;

    public long getTotalProjects() {
        return totalProjects;
    }

    public void setTotalProjects(long totalProjects) {
        this.totalProjects = totalProjects;
    }

    public Map<ProjectStatus, Long> getStatusDistribution() {
        return statusDistribution;
    }

    public void setStatusDistribution(Map<ProjectStatus, Long> statusDistribution) {
        this.statusDistribution = statusDistribution;
    }

    public Map<ProjectHealthStatus, Long> getHealthDistribution() {
        return healthDistribution;
    }

    public void setHealthDistribution(Map<ProjectHealthStatus, Long> healthDistribution) {
        this.healthDistribution = healthDistribution;
    }

    public List<DashboardProjectResponse> getProjects() {
        return projects;
    }

    public void setProjects(List<DashboardProjectResponse> projects) {
        this.projects = projects;
    }
}
