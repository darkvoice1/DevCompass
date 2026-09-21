package com.darkvoice1.devcompass.project.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.darkvoice1.devcompass.activity.entity.ActivityAction;
import com.darkvoice1.devcompass.activity.entity.ActivityObjectType;
import com.darkvoice1.devcompass.activity.service.ActivityService;
import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.project.dto.CreateProjectPhaseRequest;
import com.darkvoice1.devcompass.project.dto.ProjectPhaseResponse;
import com.darkvoice1.devcompass.project.dto.UpdateProjectPhaseRequest;
import com.darkvoice1.devcompass.project.dto.UpdateProjectPhaseSortOrderRequest;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.entity.ProjectPhase;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.project.repository.ProjectPhaseMapper;

/**
 * 处理项目阶段的创建、编辑、查询和排序业务。
 */
@Service
public class ProjectPhaseService {

    private final ProjectMapper projectMapper;

    private final ProjectPhaseMapper projectPhaseMapper;

    private final ActivityService activityService;

    /**
     * 创建项目阶段服务。
     *
     * @param projectMapper 项目数据访问对象
     * @param projectPhaseMapper 项目阶段数据访问对象
     * @param activityService 动态写入服务
     */
    public ProjectPhaseService(ProjectMapper projectMapper, ProjectPhaseMapper projectPhaseMapper,
            ActivityService activityService) {
        this.projectMapper = projectMapper;
        this.projectPhaseMapper = projectPhaseMapper;
        this.activityService = activityService;
    }

    /**
     * 在指定项目中创建阶段，新阶段默认排在最后。
     *
     * @param projectId 项目主键
     * @param request 创建阶段请求
     * @return 创建后的阶段详情
     */
    @Transactional
    public ProjectPhaseResponse createProjectPhase(Long projectId, CreateProjectPhaseRequest request) {
        ensureProjectExists(projectId);

        ProjectPhase phase = new ProjectPhase();
        phase.setProjectId(projectId);
        phase.setName(request.getName());
        phase.setDescription(request.getDescription());
        phase.setSortOrder(countProjectPhases(projectId));
        projectPhaseMapper.insert(phase);
        recordPhaseActivity(phase, ActivityAction.CREATED, "创建阶段「" + phase.getName() + "」");
        return toResponse(phase);
    }

    /**
     * 查询项目的全部阶段，并按排序序号升序返回。
     *
     * @param projectId 项目主键
     * @return 项目阶段列表
     */
    public List<ProjectPhaseResponse> getProjectPhases(Long projectId) {
        ensureProjectExists(projectId);
        return findProjectPhases(projectId).stream().map(this::toResponse).toList();
    }

    /**
     * 编辑指定项目中的阶段信息。
     *
     * @param projectId 项目主键
     * @param phaseId 阶段主键
     * @param request 编辑阶段请求
     * @return 编辑后的阶段详情
     */
    @Transactional
    public ProjectPhaseResponse updateProjectPhase(
            Long projectId, Long phaseId, UpdateProjectPhaseRequest request) {
        ensureProjectExists(projectId);
        ProjectPhase phase = findProjectPhaseOrThrow(projectId, phaseId);

        phase.setName(request.getName());
        if (request.getDescription() != null) {
            phase.setDescription(request.getDescription());
        }
        phase.setUpdatedAt(Instant.now());
        projectPhaseMapper.updateById(phase);
        recordPhaseActivity(phase, ActivityAction.UPDATED, "更新阶段「" + phase.getName() + "」");
        return toResponse(phase);
    }

    /**
     * 调整阶段位置，并重新编号同一项目内的全部阶段。
     *
     * @param projectId 项目主键
     * @param phaseId 阶段主键
     * @param request 新的排序序号
     * @return 调整后的阶段详情
     */
    @Transactional
    public ProjectPhaseResponse updateProjectPhaseSortOrder(
            Long projectId, Long phaseId, UpdateProjectPhaseSortOrderRequest request) {
        ensureProjectExists(projectId);
        ProjectPhase targetPhase = findProjectPhaseOrThrow(projectId, phaseId);
        List<ProjectPhase> phases = new ArrayList<>(findProjectPhases(projectId));
        if (request.getSortOrder() >= phases.size()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "阶段排序序号超出范围");
        }

        phases.removeIf(phase -> phase.getId().equals(targetPhase.getId()));
        phases.add(request.getSortOrder(), targetPhase);
        updateSortOrders(phases);
        recordPhaseActivity(targetPhase, ActivityAction.UPDATED,
                "调整阶段「" + targetPhase.getName() + "」的排序");
        return toResponse(targetPhase);
    }

    /**
     * 软删除项目阶段。
     *
     * @param projectId 项目主键
     * @param phaseId 阶段主键
     * @throws BusinessException 项目、阶段不存在或阶段归属不匹配时抛出
     */
    @Transactional
    public void deleteProjectPhase(Long projectId, Long phaseId) {
        ensureProjectExists(projectId);
        ProjectPhase phase = findProjectPhaseOrThrow(projectId, phaseId);
        if (projectPhaseMapper.softDeleteById(phaseId) == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "项目阶段已经删除");
        }
        recordPhaseActivity(phase, ActivityAction.DELETED, "删除阶段「" + phase.getName() + "」");
    }

    /**
     * 恢复已软删除项目阶段。
     *
     * @param projectId 项目主键
     * @param phaseId 阶段主键
     * @throws BusinessException 项目、阶段不存在或阶段归属不匹配时抛出
     */
    @Transactional
    public void restoreDeletedProjectPhase(Long projectId, Long phaseId) {
        ensureProjectExists(projectId);
        ProjectPhase phase = projectPhaseMapper.selectDeletedById(phaseId);
        if (phase == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "项目阶段不存在或未删除");
        }
        if (!projectId.equals(phase.getProjectId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "项目阶段不属于当前项目");
        }
        if (projectPhaseMapper.restoreById(phaseId) == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "项目阶段恢复失败");
        }
        recordPhaseActivity(phase, ActivityAction.RESTORED, "恢复已删除阶段「" + phase.getName() + "」");
    }

    /**
     * 记录一条阶段动态。
     */
    private void recordPhaseActivity(ProjectPhase phase, ActivityAction action, String summary) {
        activityService.record(phase.getProjectId(), ActivityObjectType.PHASE, phase.getId(),
                action, summary);
    }

    /**
     * 查询项目阶段数量，用于确定新阶段的末尾序号。
     */
    private int countProjectPhases(Long projectId) {
        Long count = projectPhaseMapper.selectCount(new QueryWrapper<ProjectPhase>()
                .eq("project_id", projectId));
        return Math.toIntExact(count);
    }

    /**
     * 查询项目全部阶段并固定排序规则。
     */
    private List<ProjectPhase> findProjectPhases(Long projectId) {
        return projectPhaseMapper.selectList(new QueryWrapper<ProjectPhase>()
                .eq("project_id", projectId)
                .orderByAsc("sort_order")
                .orderByAsc("id"));
    }

    /**
     * 查询项目阶段，并验证它确实属于当前项目。
     */
    private ProjectPhase findProjectPhaseOrThrow(Long projectId, Long phaseId) {
        ProjectPhase phase = projectPhaseMapper.selectById(phaseId);
        if (phase == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "项目阶段不存在");
        }
        if (!projectId.equals(phase.getProjectId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "项目阶段不属于当前项目");
        }
        return phase;
    }

    /**
     * 确认项目存在。
     */
    private void ensureProjectExists(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "项目不存在");
        }
    }

    /**
     * 为排序后的阶段重新设置连续序号。
     */
    private void updateSortOrders(List<ProjectPhase> phases) {
        Instant now = Instant.now();
        for (int index = 0; index < phases.size(); index++) {
            ProjectPhase phase = phases.get(index);
            if (!Integer.valueOf(index).equals(phase.getSortOrder())) {
                phase.setSortOrder(index);
                phase.setUpdatedAt(now);
                projectPhaseMapper.updateById(phase);
            }
        }
    }

    /**
     * 将项目阶段实体转换为接口响应数据。
     */
    private ProjectPhaseResponse toResponse(ProjectPhase phase) {
        ProjectPhaseResponse response = new ProjectPhaseResponse();
        response.setId(phase.getId());
        response.setProjectId(phase.getProjectId());
        response.setName(phase.getName());
        response.setDescription(phase.getDescription());
        response.setSortOrder(phase.getSortOrder());
        response.setCreatedAt(phase.getCreatedAt());
        response.setUpdatedAt(phase.getUpdatedAt());
        return response;
    }
}
