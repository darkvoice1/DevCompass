package com.darkvoice1.devcompass.project.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.darkvoice1.devcompass.common.web.ApiResponse;
import com.darkvoice1.devcompass.project.dto.CreateProjectRequest;
import com.darkvoice1.devcompass.project.dto.ProjectDetailResponse;
import com.darkvoice1.devcompass.project.service.ProjectService;

/**
 * 提供项目创建和详情查询接口。
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * 创建项目。
     *
     * @param request 创建项目请求
     * @return 创建后的项目详情
     */
    @PostMapping
    public ApiResponse<ProjectDetailResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request) {
        return ApiResponse.success(projectService.createProject(request));
    }

    /**
     * 查询指定项目的详情。
     *
     * @param projectId 项目主键
     * @return 项目详情
     */
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectDetailResponse> getProjectDetail(@PathVariable Long projectId) {
        return ApiResponse.success(projectService.getProjectDetail(projectId));
    }
}
