package com.darkvoice1.devcompass.project.service;

import org.springframework.stereotype.Service;

import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.project.dto.CreateProjectRequest;
import com.darkvoice1.devcompass.project.dto.ProjectDetailResponse;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.entity.ProjectStatus;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;

/**
 * 处理项目创建和查询业务。
 */
@Service
public class ProjectService {

    private final ProjectMapper projectMapper;

    public ProjectService(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    /**
     * 创建项目并返回完整详情。
     *
     * @param request 创建项目的请求参数
     * @return 创建后的项目详情
     */
    public ProjectDetailResponse createProject(CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStatus(request.getStatus() == null ? ProjectStatus.PLANNED : request.getStatus());
        project.setTargetDate(request.getTargetDate());
        project.setTechStack(request.getTechStack());

        projectMapper.insert(project);
        return getProjectDetail(project.getId());
    }

    /**
     * 根据主键查询项目详情。
     *
     * @param projectId 项目主键
     * @return 项目详情
     * @throws BusinessException 项目不存在时抛出
     */
    public ProjectDetailResponse getProjectDetail(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "项目不存在");
        }
        return toResponse(project);
    }

    /**
     * 将项目实体转换为接口响应数据。
     *
     * @param project 项目实体
     * @return 项目详情响应
     */
    private ProjectDetailResponse toResponse(Project project) {
        ProjectDetailResponse response = new ProjectDetailResponse();
        response.setId(project.getId());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setStatus(project.getStatus());
        response.setTargetDate(project.getTargetDate());
        response.setTechStack(project.getTechStack());
        response.setArchived(project.isArchived());
        response.setArchivedAt(project.getArchivedAt());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());
        return response;
    }
}
