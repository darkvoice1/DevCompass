package com.darkvoice1.devcompass.project.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
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
}
