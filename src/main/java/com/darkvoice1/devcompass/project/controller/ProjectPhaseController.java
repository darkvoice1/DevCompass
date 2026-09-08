package com.darkvoice1.devcompass.project.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.darkvoice1.devcompass.common.web.ApiResponse;
import com.darkvoice1.devcompass.project.dto.CreateProjectPhaseRequest;
import com.darkvoice1.devcompass.project.dto.ProjectPhaseResponse;
import com.darkvoice1.devcompass.project.dto.UpdateProjectPhaseRequest;
import com.darkvoice1.devcompass.project.dto.UpdateProjectPhaseSortOrderRequest;
import com.darkvoice1.devcompass.project.service.ProjectPhaseService;

/**
 * 提供项目阶段的创建、编辑、查询和排序接口。
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/phases")
public class ProjectPhaseController {

    private final ProjectPhaseService projectPhaseService;

    /**
     * 创建项目阶段控制器。
     *
     * @param projectPhaseService 项目阶段业务服务
     */
    public ProjectPhaseController(ProjectPhaseService projectPhaseService) {
        this.projectPhaseService = projectPhaseService;
    }

    /**
     * 在项目中创建阶段。
     */
    @PostMapping
    public ApiResponse<ProjectPhaseResponse> createProjectPhase(
            @PathVariable Long projectId, @Valid @RequestBody CreateProjectPhaseRequest request) {
        return ApiResponse.success(projectPhaseService.createProjectPhase(projectId, request));
    }

    /**
     * 查询项目下的阶段列表。
     */
    @GetMapping
    public ApiResponse<List<ProjectPhaseResponse>> getProjectPhases(@PathVariable Long projectId) {
        return ApiResponse.success(projectPhaseService.getProjectPhases(projectId));
    }

    /**
     * 编辑项目中的阶段。
     */
    @PutMapping("/{phaseId}")
    public ApiResponse<ProjectPhaseResponse> updateProjectPhase(
            @PathVariable Long projectId, @PathVariable Long phaseId,
            @Valid @RequestBody UpdateProjectPhaseRequest request) {
        return ApiResponse.success(projectPhaseService.updateProjectPhase(projectId, phaseId, request));
    }

    /**
     * 调整项目中阶段的排序位置。
     */
    @PutMapping("/{phaseId}/sort-order")
    public ApiResponse<ProjectPhaseResponse> updateProjectPhaseSortOrder(
            @PathVariable Long projectId, @PathVariable Long phaseId,
            @Valid @RequestBody UpdateProjectPhaseSortOrderRequest request) {
        return ApiResponse.success(
                projectPhaseService.updateProjectPhaseSortOrder(projectId, phaseId, request));
    }
}
