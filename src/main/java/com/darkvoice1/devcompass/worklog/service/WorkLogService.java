package com.darkvoice1.devcompass.worklog.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
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

    /**
     * 创建工作日志服务。
     *
     * @param workLogMapper 工作日志数据访问对象
     */
    public WorkLogService(WorkLogMapper workLogMapper) {
        this.workLogMapper = workLogMapper;
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
     * 将请求中的日志内容写入实体。
     */
    private void applyContent(WorkLog workLog, WorkLogContentRequest request) {
        workLog.setLogDate(request.getLogDate());
        workLog.setPlanContent(request.getPlanContent());
        workLog.setSummaryContent(request.getSummaryContent());
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
        response.setSpentMinutes(workLog.getSpentMinutes());
        response.setBlockerReason(workLog.getBlockerReason());
        response.setCreatedAt(workLog.getCreatedAt());
        response.setUpdatedAt(workLog.getUpdatedAt());
        return response;
    }
}
