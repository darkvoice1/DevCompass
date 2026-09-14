package com.darkvoice1.devcompass.project.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.project.dto.ProjectProgressResponse;
import com.darkvoice1.devcompass.project.dto.UpdateProjectProgressRequest;
import com.darkvoice1.devcompass.project.entity.ProgressMode;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.task.entity.Task;
import com.darkvoice1.devcompass.task.entity.TaskStatus;
import com.darkvoice1.devcompass.task.repository.TaskMapper;

/**
 * 负责根据任务完成情况计算项目自动进度。
 */
@Service
public class ProgressService {

    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;

    /**
     * 创建项目进度服务。
     *
     * @param projectMapper 项目数据访问对象
     * @param taskMapper 任务数据访问对象
     */
    public ProgressService(ProjectMapper projectMapper, TaskMapper taskMapper) {
        this.projectMapper = projectMapper;
        this.taskMapper = taskMapper;
    }

    /**
     * 按未软删除任务中的已完成任务比例计算自动进度。
     *
     * @param projectId 项目主键
     * @return 0 到 100 之间的自动进度
     * @throws BusinessException 项目不存在时抛出
     */
    public int calculateAutoProgress(Long projectId) {
        ensureProjectExists(projectId);
        return calculateProgressWithoutProjectCheck(projectId);
    }

    /**
     * 重新计算并保存项目自动进度。
     *
     * @param projectId 项目主键
     * @return 保存后的自动进度
     * @throws BusinessException 项目不存在时抛出
     */
    public int refreshAutoProgress(Long projectId) {
        Project project = findProjectOrThrow(projectId);
        int progress = calculateProgressWithoutProjectCheck(projectId);
        project.setAutoProgress(progress);
        project.setUpdatedAt(Instant.now());
        projectMapper.updateById(project);
        return progress;
    }

    /**
     * 切换进度模式，或保存人工校准进度及其原因。
     *
     * @param projectId 项目主键
     * @param request 进度更新请求
     * @return 更新后的项目进度数据
     * @throws BusinessException 项目不存在或人工校准参数不合法时抛出
     */
    public ProjectProgressResponse updateProjectProgress(
            Long projectId, UpdateProjectProgressRequest request) {
        Project project = findProjectOrThrow(projectId);
        if (request == null || request.getMode() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "进度模式不能为空");
        }

        if (request.getMode() == ProgressMode.MANUAL) {
            validateManualProgressRequest(request);
            project.setProgressMode(ProgressMode.MANUAL);
            project.setManualProgress(request.getManualProgress());
            project.setProgressReason(request.getProgressReason().trim());
        } else {
            project.setProgressMode(ProgressMode.AUTO);
            project.setAutoProgress(calculateProgressWithoutProjectCheck(projectId));
            // 取消校准后，清除已不再生效的人工数据。
            project.setManualProgress(null);
            project.setProgressReason(null);
        }
        project.setUpdatedAt(Instant.now());
        projectMapper.updateById(project);
        return toProgressResponse(project);
    }

    /**
     * 执行任务数量统计，MyBatis-Plus 会自动排除软删除任务。
     */
    private int calculateProgressWithoutProjectCheck(Long projectId) {
        Long total = taskMapper.selectCount(new QueryWrapper<Task>()
                .eq("project_id", projectId));
        if (total == null || total == 0) {
            return 0;
        }

        Long completed = taskMapper.selectCount(new QueryWrapper<Task>()
                .eq("project_id", projectId)
                .eq("status", TaskStatus.COMPLETED));
        long completedCount = completed == null ? 0 : completed;
        long progress = completedCount * 100L / total;
        return (int) Math.max(0, Math.min(100, progress));
    }

    /**
     * 查询项目并统一处理不存在的情况。
     */
    private Project findProjectOrThrow(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "项目不存在");
        }
        return project;
    }

    /**
     * 校验项目存在。
     */
    private void ensureProjectExists(Long projectId) {
        findProjectOrThrow(projectId);
    }

    /**
     * 校验人工模式必填的进度值和校准原因。
     */
    private void validateManualProgressRequest(UpdateProjectProgressRequest request) {
        Integer manualProgress = request.getManualProgress();
        if (manualProgress == null || manualProgress < 0 || manualProgress > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "人工进度必须在0到100之间");
        }
        if (request.getProgressReason() == null || request.getProgressReason().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "手动模式必须填写校准原因");
        }
        if (request.getProgressReason().length() > 500) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "校准原因长度不能超过500个字符");
        }
    }

    /**
     * 将项目中的进度字段转换为接口响应数据。
     */
    private ProjectProgressResponse toProgressResponse(Project project) {
        ProjectProgressResponse response = new ProjectProgressResponse();
        response.setProjectId(project.getId());
        response.setMode(project.getProgressMode());
        response.setProgress(project.getProgressMode() == ProgressMode.MANUAL
                ? project.getManualProgress() : project.getAutoProgress());
        response.setAutoProgress(project.getAutoProgress());
        response.setManualProgress(project.getManualProgress());
        response.setProgressReason(project.getProgressReason());
        return response;
    }
}
