package com.darkvoice1.devcompass.task.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.entity.ProjectPhase;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.project.repository.ProjectPhaseMapper;
import com.darkvoice1.devcompass.task.dto.CreateTaskRequest;
import com.darkvoice1.devcompass.task.dto.ChangeTaskStatusRequest;
import com.darkvoice1.devcompass.task.dto.TaskDetailResponse;
import com.darkvoice1.devcompass.task.dto.TaskBoardColumnResponse;
import com.darkvoice1.devcompass.task.dto.TaskBoardResponse;
import com.darkvoice1.devcompass.task.dto.UpdateTaskRequest;
import com.darkvoice1.devcompass.task.entity.Task;
import com.darkvoice1.devcompass.task.entity.TaskPriority;
import com.darkvoice1.devcompass.task.entity.TaskStatus;
import com.darkvoice1.devcompass.task.repository.TaskMapper;

/**
 * 处理任务创建、编辑、状态变更和查询业务。
 */
@Service
public class TaskService {

    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;
    private final ProjectPhaseMapper projectPhaseMapper;

    /**
     * 创建任务服务。
     *
     * @param taskMapper 任务数据访问对象
     * @param projectMapper 项目数据访问对象
     * @param projectPhaseMapper 项目阶段数据访问对象
     */
    public TaskService(TaskMapper taskMapper, ProjectMapper projectMapper,
            ProjectPhaseMapper projectPhaseMapper) {
        this.taskMapper = taskMapper;
        this.projectMapper = projectMapper;
        this.projectPhaseMapper = projectPhaseMapper;
    }

    /**
     * 创建任务。
     *
     * @param request 创建任务请求
     * @return 创建后的任务详情
     */
    public TaskDetailResponse createTask(CreateTaskRequest request) {
        ensureProjectExists(request.getProjectId());
        ensurePhaseBelongsToProject(request.getProjectId(), request.getPhaseId());
        Task task = new Task();
        task.setProjectId(request.getProjectId());
        task.setPhaseId(request.getPhaseId());
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(TaskStatus.TODO);
        task.setPriority(request.getPriority() == null ? TaskPriority.MEDIUM : request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setEstimatedHours(request.getEstimatedHours());
        taskMapper.insert(task);
        return toResponse(task);
    }

    /**
     * 编辑任务字段。
     *
     * @param taskId 任务主键
     * @param request 编辑任务请求
     * @return 更新后的任务详情
     */
    public TaskDetailResponse updateTask(Long taskId, UpdateTaskRequest request) {
        Task task = findTaskOrThrow(taskId);
        task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());
        if (request.getEstimatedHours() != null) task.setEstimatedHours(request.getEstimatedHours());
        task.setUpdatedAt(Instant.now());
        taskMapper.updateById(task);
        return toResponse(task);
    }

    /**
     * 按统一流转规则变更任务状态。
     *
     * @param taskId 任务主键
     * @param request 状态变更请求
     * @return 变更后的任务详情
     * @throws BusinessException 状态流转不合法或任务状态发生并发变化时抛出
     */
    public TaskDetailResponse changeTaskStatus(Long taskId, ChangeTaskStatusRequest request) {
        Task task = findTaskOrThrow(taskId);
        TaskStatus currentStatus = task.getStatus();
        TaskStatus targetStatus = request.getTargetStatus();

        if (targetStatus != null && currentStatus == targetStatus) {
            return toResponse(task);
        }
        if (currentStatus == null || !currentStatus.canTransitionTo(targetStatus)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "任务状态不能从 " + currentStatus + " 流转到 " + targetStatus);
        }

        Instant updatedAt = Instant.now();
        if (taskMapper.updateStatusIfCurrent(taskId, currentStatus, targetStatus, updatedAt) == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "任务状态已发生变化，请重试");
        }

        task.setStatus(targetStatus);
        task.setUpdatedAt(updatedAt);
        return toResponse(task);
    }

    /**
     * 软删除任务，不物理移除数据库记录。
     *
     * @param taskId 任务主键
     * @throws BusinessException 任务不存在或已删除时抛出
     */
    public void deleteTask(Long taskId) {
        if (taskMapper.softDeleteById(taskId) == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "任务不存在或已经删除");
        }
    }

    /**
     * 恢复已软删除任务。
     *
     * @param taskId 任务主键
     * @throws BusinessException 任务不存在或未删除时抛出
     */
    public void restoreDeletedTask(Long taskId) {
        if (taskMapper.restoreById(taskId) == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "任务不存在或未删除");
        }
    }

    /**
     * 按项目和可选条件查询任务。
     *
     * @param projectId 项目主键
     * @param status 任务状态，可为空
     * @param priority 任务优先级，可为空
     * @param keyword 任务标题关键字，可为空
     * @return 匹配的任务列表
     */
    public List<TaskDetailResponse> queryTasks(Long projectId, TaskStatus status,
            TaskPriority priority, String keyword) {
        ensureProjectExists(projectId);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Task>()
                .eq("project_id", projectId);
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (priority != null) {
            wrapper.eq("priority", priority);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("title", keyword.trim());
        }
        wrapper.orderByAsc("due_date").orderByDesc("updated_at");
        return taskMapper.selectList(wrapper).stream().map(this::toResponse).toList();
    }

    /**
     * 查询项目任务看板，始终返回全部状态列。
     *
     * @param projectId 项目主键
     * @return 按状态分组的任务看板
     * @throws BusinessException 项目不存在时抛出
     */
    public TaskBoardResponse getTaskBoard(Long projectId) {
        ensureProjectExists(projectId);

        List<ProjectPhase> projectPhases = projectPhaseMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ProjectPhase>()
                        .eq("project_id", projectId)
                        .orderByAsc("sort_order")
                        .orderByAsc("id"));
        Set<Long> activePhaseIds = new LinkedHashSet<>();
        for (ProjectPhase phase : projectPhases) {
            if (phase != null && phase.getDeletedAt() == null && phase.getId() != null) {
                activePhaseIds.add(phase.getId());
            }
        }

        Map<TaskStatus, List<TaskDetailResponse>> tasksByStatus = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            tasksByStatus.put(status, new ArrayList<>());
        }

        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Task>()
                .eq("project_id", projectId)
                .orderByAsc("phase_id")
                .orderByAsc("due_date")
                .orderByDesc("updated_at");
        for (Task task : taskMapper.selectList(wrapper)) {
            if (task.getDeletedAt() != null
                    || !activePhaseIds.contains(task.getPhaseId())
                    || task.getStatus() == null) {
                continue;
            }
            tasksByStatus.get(task.getStatus()).add(toResponse(task));
        }

        TaskBoardResponse response = new TaskBoardResponse();
        response.setProjectId(projectId);
        response.setColumns(Arrays.stream(TaskStatus.values()).map(status -> {
            TaskBoardColumnResponse column = new TaskBoardColumnResponse();
            List<TaskDetailResponse> tasks = List.copyOf(tasksByStatus.get(status));
            column.setStatus(status);
            column.setTasks(tasks);
            column.setCount(tasks.size());
            return column;
        }).toList());
        return response;
    }

    /**
     * 查询任务，不存在时统一抛出业务异常。
     *
     * @param taskId 任务主键
     * @return 任务实体
     * @throws BusinessException 任务不存在时抛出
     */
    private Task findTaskOrThrow(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(ErrorCode.BUSINESS_ERROR, "任务不存在");
        return task;
    }

    /**
     * 校验项目是否存在。
     *
     * @param projectId 项目主键
     * @throws BusinessException 项目不存在时抛出
     */
    private void ensureProjectExists(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BusinessException(ErrorCode.BUSINESS_ERROR, "项目不存在");
    }

    /**
     * 校验阶段存在且属于当前项目。
     *
     * @param projectId 项目主键
     * @param phaseId 阶段主键
     * @throws BusinessException 阶段不存在或归属不一致时抛出
     */
    private void ensurePhaseBelongsToProject(Long projectId, Long phaseId) {
        ProjectPhase phase = projectPhaseMapper.selectById(phaseId);
        if (phase == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "项目阶段不存在");
        }
        if (!projectId.equals(phase.getProjectId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "任务阶段不属于当前项目");
        }
    }

    /**
     * 将任务实体转换为接口响应数据。
     *
     * @param task 任务实体
     * @return 任务详情响应
     */
    private TaskDetailResponse toResponse(Task task) {
        ProjectPhase phase = projectPhaseMapper.selectById(task.getPhaseId());
        TaskDetailResponse response = new TaskDetailResponse();
        response.setId(task.getId());
        response.setProjectId(task.getProjectId());
        response.setPhaseId(task.getPhaseId());
        response.setPhaseName(phase == null ? null : phase.getName());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus());
        response.setPriority(task.getPriority());
        response.setDueDate(task.getDueDate());
        response.setEstimatedHours(task.getEstimatedHours());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        return response;
    }
}
