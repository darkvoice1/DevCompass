package com.darkvoice1.devcompass.worklog.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.task.entity.Task;
import com.darkvoice1.devcompass.task.repository.TaskMapper;
import com.darkvoice1.devcompass.worklog.dto.WorkLogContentRequest;
import com.darkvoice1.devcompass.worklog.dto.WorkLogDateRangeQueryRequest;
import com.darkvoice1.devcompass.worklog.dto.WorkLogResponse;
import com.darkvoice1.devcompass.worklog.entity.WorkLog;
import com.darkvoice1.devcompass.worklog.repository.WorkLogMapper;

/**
 * 处理任务完成日志的保存、查看和编辑业务。
 */
@Service
public class WorkLogService {

    private final WorkLogMapper workLogMapper;
    private final TaskMapper taskMapper;

    /**
     * 创建工作日志服务。
     *
     * @param workLogMapper 工作日志数据访问对象
     * @param taskMapper 任务数据访问对象
     */
    public WorkLogService(WorkLogMapper workLogMapper, TaskMapper taskMapper) {
        this.workLogMapper = workLogMapper;
        this.taskMapper = taskMapper;
    }

    /**
     * 保存任务完成时填写的日志；任务重新完成时更新原有日志。
     *
     * @param taskId 任务主键
     * @param request 日志内容
     * @return 保存后的日志详情
     */
    public WorkLogResponse saveCompletionLog(Long taskId, WorkLogContentRequest request) {
        WorkLog workLog = findByTaskId(taskId);
        if (workLog == null) {
            workLog = new WorkLog();
            workLog.setTaskId(taskId);
            applyContent(workLog, request);
            workLogMapper.insert(workLog);
        } else {
            applyContent(workLog, request);
            workLog.setUpdatedAt(Instant.now());
            workLogMapper.updateById(workLog);
        }
        return toResponse(workLog);
    }

    /**
     * 查询指定任务的工作日志。
     *
     * @param taskId 任务主键
     * @return 工作日志详情
     */
    public WorkLogResponse getWorkLogByTaskId(Long taskId) {
        WorkLog workLog = findByTaskId(taskId);
        if (workLog == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "任务暂无工作日志");
        }
        return toResponse(workLog);
    }

    /**
     * 按日志日期范围查询工作日志，并按最近日志优先返回。
     *
     * @param request 日期范围查询参数
     * @return 工作日志列表
     */
    public List<WorkLogResponse> queryWorkLogsByDateRange(WorkLogDateRangeQueryRequest request) {
        validateDateRange(request.getLogDateFrom(), request.getLogDateTo());
        return workLogMapper.selectList(new QueryWrapper<WorkLog>()
                .ge("log_date", request.getLogDateFrom())
                .le("log_date", request.getLogDateTo())
                .orderByDesc("log_date")
                .orderByDesc("updated_at")
                .orderByDesc("id"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 将指定日期范围内的工作日志导出为 Markdown 内容。
     *
     * @param request 日期范围查询参数
     * @return Markdown 格式的工作日志内容
     */
    public String exportWorkLogs(WorkLogDateRangeQueryRequest request) {
        List<WorkLogResponse> workLogs = queryWorkLogsByDateRange(request);
        StringBuilder markdown = new StringBuilder("# 工作日志\n\n")
                .append("日期范围：")
                .append(request.getLogDateFrom())
                .append(" 至 ")
                .append(request.getLogDateTo())
                .append("\n\n");
        LocalDate currentDate = null;
        for (WorkLogResponse workLog : workLogs) {
            if (!workLog.getLogDate().equals(currentDate)) {
                currentDate = workLog.getLogDate();
                markdown.append("## ").append(currentDate).append("\n\n");
            }
            markdown.append("### 任务 #")
                    .append(workLog.getTaskId())
                    .append("：")
                    .append(resolveTaskTitle(workLog.getTaskId()))
                    .append("\n\n")
                    .append("- 计划：").append(displayContent(workLog.getPlanContent())).append("\n")
                    .append("- 完成总结：").append(workLog.getSummaryContent()).append("\n")
                    .append("- 提交记录：").append(displayContent(workLog.getCommitHashes())).append("\n")
                    .append("- 实际耗时：").append(workLog.getSpentMinutes()).append(" 分钟\n")
                    .append("- 阻塞原因：").append(displayContent(workLog.getBlockerReason()))
                    .append("\n\n");
        }
        return markdown.toString();
    }

    /**
     * 编辑已创建的工作日志。
     *
     * @param workLogId 工作日志主键
     * @param request 日志内容
     * @return 编辑后的日志详情
     */
    public WorkLogResponse updateWorkLog(Long workLogId, WorkLogContentRequest request) {
        WorkLog workLog = workLogMapper.selectById(workLogId);
        if (workLog == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "工作日志不存在");
        }
        applyContent(workLog, request);
        workLog.setUpdatedAt(Instant.now());
        workLogMapper.updateById(workLog);
        return toResponse(workLog);
    }

    /**
     * 按任务查询日志，数据库唯一约束保证最多返回一条记录。
     */
    private WorkLog findByTaskId(Long taskId) {
        return workLogMapper.selectOne(new QueryWrapper<WorkLog>().eq("task_id", taskId));
    }

    /**
     * 校验日期范围的起止顺序。
     */
    private void validateDateRange(LocalDate logDateFrom, LocalDate logDateTo) {
        if (logDateFrom == null || logDateTo == null || logDateFrom.isAfter(logDateTo)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "日志日期范围不合法");
        }
    }

    /**
     * 查询任务标题，已删除任务保留可识别的导出标记。
     */
    private String resolveTaskTitle(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        return task == null ? "已删除任务" : task.getTitle();
    }

    /**
     * 将空白的可选内容展示为“无”。
     */
    private String displayContent(String content) {
        return content == null || content.isBlank() ? "无" : content;
    }

    /**
     * 统一提交短哈希列表的分隔格式。
     */
    private String normalizeCommitHashes(String commitHashes) {
        return Arrays.stream(commitHashes.split(","))
                .map(String::trim)
                .collect(Collectors.joining(","));
    }

    /**
     * 将请求中的日志内容写入实体。
     */
    private void applyContent(WorkLog workLog, WorkLogContentRequest request) {
        workLog.setLogDate(request.getLogDate());
        workLog.setPlanContent(request.getPlanContent());
        workLog.setSummaryContent(request.getSummaryContent());
        workLog.setCommitHashes(normalizeCommitHashes(request.getCommitHashes()));
        workLog.setSpentMinutes(request.getSpentMinutes());
        workLog.setBlockerReason(request.getBlockerReason());
    }

    /**
     * 将工作日志实体转换为接口响应数据。
     */
    private WorkLogResponse toResponse(WorkLog workLog) {
        WorkLogResponse response = new WorkLogResponse();
        response.setId(workLog.getId());
        response.setTaskId(workLog.getTaskId());
        response.setLogDate(workLog.getLogDate());
        response.setPlanContent(workLog.getPlanContent());
        response.setSummaryContent(workLog.getSummaryContent());
        response.setCommitHashes(workLog.getCommitHashes());
        response.setSpentMinutes(workLog.getSpentMinutes());
        response.setBlockerReason(workLog.getBlockerReason());
        response.setCreatedAt(workLog.getCreatedAt());
        response.setUpdatedAt(workLog.getUpdatedAt());
        return response;
    }
}
