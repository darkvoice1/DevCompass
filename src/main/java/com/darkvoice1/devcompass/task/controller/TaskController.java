package com.darkvoice1.devcompass.task.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.darkvoice1.devcompass.common.web.ApiResponse;
import com.darkvoice1.devcompass.task.dto.CreateTaskRequest;
import com.darkvoice1.devcompass.task.dto.ChangeTaskStatusRequest;
import com.darkvoice1.devcompass.task.dto.TaskDetailResponse;
import com.darkvoice1.devcompass.task.dto.TaskBoardResponse;
import com.darkvoice1.devcompass.task.dto.UpdateTaskRequest;
import com.darkvoice1.devcompass.task.entity.TaskPriority;
import com.darkvoice1.devcompass.task.entity.TaskStatus;
import com.darkvoice1.devcompass.task.service.TaskService;

/**
 * 提供任务创建、编辑、状态变更和查询接口。
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    /**
     * 创建任务控制器。
     *
     * @param taskService 任务业务服务
     */
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 创建任务。
     *
     * @param request 创建任务请求
     * @return 任务详情
     */
    @PostMapping
    public ApiResponse<TaskDetailResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.success(taskService.createTask(request));
    }

    /**
     * 编辑任务。
     *
     * @param taskId 任务主键
     * @param request 编辑任务请求
     * @return 更新后的任务详情
     */
    @PutMapping("/{taskId}")
    public ApiResponse<TaskDetailResponse> updateTask(
            @PathVariable Long taskId, @Valid @RequestBody UpdateTaskRequest request) {
        return ApiResponse.success(taskService.updateTask(taskId, request));
    }

    /**
     * 变更任务状态。
     *
     * @param taskId 任务主键
     * @param request 状态变更请求
     * @return 更新后的任务详情
     */
    @PatchMapping("/{taskId}/status")
    public ApiResponse<TaskDetailResponse> changeTaskStatus(
            @PathVariable Long taskId, @Valid @RequestBody ChangeTaskStatusRequest request) {
        return ApiResponse.success(taskService.changeTaskStatus(taskId, request));
    }

    /**
     * 按项目和可选条件查询任务。
     *
     * @param projectId 项目主键
     * @param status 任务状态，可为空
     * @param priority 任务优先级，可为空
     * @param keyword 任务标题关键字，可为空
     * @return 任务详情列表
     */
    @GetMapping
    public ApiResponse<List<TaskDetailResponse>> queryTasks(
            @RequestParam(name = "projectId") Long projectId,
            @RequestParam(name = "status", required = false) TaskStatus status,
            @RequestParam(name = "priority", required = false) TaskPriority priority,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return ApiResponse.success(taskService.queryTasks(projectId, status, priority, keyword));
    }

    /**
     * 查询项目任务看板。
     *
     * @param projectId 项目主键
     * @return 按任务状态分组的看板数据
     */
    @GetMapping("/board")
    public ApiResponse<TaskBoardResponse> getTaskBoard(
            @RequestParam(name = "projectId") Long projectId) {
        return ApiResponse.success(taskService.getTaskBoard(projectId));
    }

    /**
     * 软删除任务。
     *
     * @param taskId 任务主键
     * @return 空响应
     */
    @DeleteMapping("/{taskId}")
    public ApiResponse<Void> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ApiResponse.success(null);
    }

    /**
     * 恢复已软删除任务。
     *
     * @param taskId 任务主键
     * @return 空响应
     */
    @PostMapping("/{taskId}/restore-deleted")
    public ApiResponse<Void> restoreDeletedTask(@PathVariable Long taskId) {
        taskService.restoreDeletedTask(taskId);
        return ApiResponse.success(null);
    }

}
