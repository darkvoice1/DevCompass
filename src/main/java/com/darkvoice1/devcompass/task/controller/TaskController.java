package com.darkvoice1.devcompass.task.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.darkvoice1.devcompass.common.web.ApiResponse;
import com.darkvoice1.devcompass.task.dto.CreateTaskRequest;
import com.darkvoice1.devcompass.task.dto.TaskDetailResponse;
import com.darkvoice1.devcompass.task.dto.UpdateTaskRequest;
import com.darkvoice1.devcompass.task.service.TaskService;

/**
 * 提供任务创建和编辑接口。
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

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

}
