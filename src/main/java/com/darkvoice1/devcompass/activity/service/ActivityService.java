package com.darkvoice1.devcompass.activity.service;

import org.springframework.stereotype.Service;

import com.darkvoice1.devcompass.activity.entity.Activity;
import com.darkvoice1.devcompass.activity.entity.ActivityAction;
import com.darkvoice1.devcompass.activity.entity.ActivityObjectType;
import com.darkvoice1.devcompass.activity.repository.ActivityMapper;
import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;

/**
 * 写入项目动态记录。
 */
@Service
public class ActivityService {

    /**
     * 动态摘要最长字符数。
     */
    public static final int SUMMARY_MAX_LENGTH = 500;

    private final ActivityMapper activityMapper;

    /**
     * 创建动态服务。
     *
     * @param activityMapper 动态数据访问对象
     */
    public ActivityService(ActivityMapper activityMapper) {
        this.activityMapper = activityMapper;
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
}
