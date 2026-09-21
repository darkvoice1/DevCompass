package com.darkvoice1.devcompass.activity.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.darkvoice1.devcompass.activity.dto.ActivityPageResponse;
import com.darkvoice1.devcompass.activity.dto.ActivityQueryRequest;
import com.darkvoice1.devcompass.activity.dto.ActivityResponse;
import com.darkvoice1.devcompass.activity.entity.Activity;
import com.darkvoice1.devcompass.activity.entity.ActivityAction;
import com.darkvoice1.devcompass.activity.entity.ActivityObjectType;
import com.darkvoice1.devcompass.activity.repository.ActivityMapper;
import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;

/**
 * 写入和查询项目动态记录。
 */
@Service
public class ActivityService {

    /**
     * 动态摘要最长字符数。
     */
    public static final int SUMMARY_MAX_LENGTH = 500;

    private final ActivityMapper activityMapper;

    private final Clock clock;

    /**
     * 创建动态服务。
     *
     * @param activityMapper 动态数据访问对象
     * @param clock 应用时钟，用于按统一时区解释日期
     */
    public ActivityService(ActivityMapper activityMapper, Clock clock) {
        this.activityMapper = activityMapper;
        this.clock = clock;
    }

    /**
     * 保存一条动态。
     *
     * @param projectId 所属项目
     * @param objectType 对象类型
     * @param objectId 对象主键
     * @param action 操作类型
     * @param summary 中文摘要
     * @return 保存后的动态
     */
    public Activity record(Long projectId, ActivityObjectType objectType, Long objectId,
            ActivityAction action, String summary) {
        if (projectId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "项目不能为空");
        }
        if (objectType == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "对象类型不能为空");
        }
        if (objectId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "对象不能为空");
        }
        if (action == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "动作不能为空");
        }
        String normalizedSummary = summary == null ? null : summary.trim();
        if (normalizedSummary == null || normalizedSummary.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "动态摘要不能为空");
        }
        if (normalizedSummary.length() > SUMMARY_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "动态摘要长度不能超过500个字符");
        }

        Activity activity = new Activity();
        activity.setProjectId(projectId);
        activity.setObjectType(objectType);
        activity.setObjectId(objectId);
        activity.setAction(action);
        activity.setSummary(normalizedSummary);
        activityMapper.insert(activity);
        return activity;
    }

    /**
     * 按项目分页查询动态，可按对象类型和日期筛选，结果从新到旧。
     *
     * @param request 查询参数
     * @return 动态分页结果
     */
    public ActivityPageResponse queryActivities(ActivityQueryRequest request) {
        validateQueryRequest(request);

        QueryWrapper<Activity> wrapper = new QueryWrapper<Activity>()
                .eq("project_id", request.getProjectId());
        if (request.getObjectType() != null) {
            wrapper.eq("object_type", request.getObjectType());
        }
        Instant createdFrom = startOfDay(request.getDateFrom());
        Instant createdTo = startOfNextDay(request.getDateTo());
        if (createdFrom != null) {
            wrapper.ge("created_at", createdFrom);
        }
        if (createdTo != null) {
            wrapper.lt("created_at", createdTo);
        }
        wrapper.orderByDesc("created_at").orderByDesc("id");

        Page<Activity> page = new Page<>(request.getPage(), request.getPageSize());
        Page<Activity> result = activityMapper.selectPage(page, wrapper);

        ActivityPageResponse response = new ActivityPageResponse();
        response.setRecords(result.getRecords().stream().map(this::toResponse).toList());
        response.setTotal(result.getTotal());
        response.setPage(result.getCurrent());
        response.setPageSize(result.getSize());
        response.setTotalPages(result.getPages());
        return response;
    }

    /**
     * 校验分页参数和日期范围。
     */
    private void validateQueryRequest(ActivityQueryRequest request) {
        if (request == null || request.getProjectId() == null || request.getProjectId() < 1
                || request.getPage() == null || request.getPage() < 1
                || request.getPage() > 1_000_000L
                || request.getPageSize() == null || request.getPageSize() < 1
                || request.getPageSize() > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "分页参数不合法");
        }
        LocalDate dateFrom = request.getDateFrom();
        LocalDate dateTo = request.getDateTo();
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "日期范围不合法");
        }
    }

    /**
     * 把日历日转换成该时区当天 0 点对应的时间。
     */
    private Instant startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay(clock.getZone()).toInstant();
    }

    /**
     * 把日历日转换成次日 0 点，用作含当天的开区间终点。
     */
    private Instant startOfNextDay(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
    }

    /**
     * 将动态实体转换为接口响应。
     */
    private ActivityResponse toResponse(Activity activity) {
        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setProjectId(activity.getProjectId());
        response.setObjectType(activity.getObjectType());
        response.setObjectId(activity.getObjectId());
        response.setAction(activity.getAction());
        response.setSummary(activity.getSummary());
        response.setCreatedAt(activity.getCreatedAt());
        return response;
    }
}
