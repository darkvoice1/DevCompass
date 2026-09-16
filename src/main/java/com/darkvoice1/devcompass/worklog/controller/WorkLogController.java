package com.darkvoice1.devcompass.worklog.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.darkvoice1.devcompass.common.web.ApiResponse;
import com.darkvoice1.devcompass.worklog.dto.WorkLogContentRequest;
import com.darkvoice1.devcompass.worklog.dto.WorkLogResponse;
import com.darkvoice1.devcompass.worklog.service.WorkLogService;

/**
 * 提供工作日志查看和编辑接口。
 */
@RestController
@RequestMapping("/api/v1/work-logs")
public class WorkLogController {

    private final WorkLogService workLogService;

    /**
     * 创建工作日志控制器。
     *
     * @param workLogService 工作日志业务服务
     */
    public WorkLogController(WorkLogService workLogService) {
        this.workLogService = workLogService;
    }

    /**
     * 查询指定任务的工作日志。
     *
     * @param taskId 任务主键
     * @return 工作日志详情
     */
    @GetMapping("/tasks/{taskId}")
    public ApiResponse<WorkLogResponse> getWorkLogByTaskId(@PathVariable Long taskId) {
        return ApiResponse.success(workLogService.getWorkLogByTaskId(taskId));
    }

    /**
     * 编辑指定工作日志。
     *
     * @param workLogId 工作日志主键
     * @param request 日志内容
     * @return 编辑后的日志详情
     */
    @PutMapping("/{workLogId}")
    public ApiResponse<WorkLogResponse> updateWorkLog(
            @PathVariable Long workLogId, @Valid @RequestBody WorkLogContentRequest request) {
        return ApiResponse.success(workLogService.updateWorkLog(workLogId, request));
    }
}
