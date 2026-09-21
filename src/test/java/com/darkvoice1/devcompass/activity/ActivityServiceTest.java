package com.darkvoice1.devcompass.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.darkvoice1.devcompass.activity.entity.Activity;
import com.darkvoice1.devcompass.activity.entity.ActivityAction;
import com.darkvoice1.devcompass.activity.entity.ActivityObjectType;
import com.darkvoice1.devcompass.activity.repository.ActivityMapper;
import com.darkvoice1.devcompass.activity.service.ActivityService;
import com.darkvoice1.devcompass.common.exception.BusinessException;

/**
 * 验证动态写入时的必填校验和摘要处理。
 */
class ActivityServiceTest {

    private ActivityMapper activityMapper;

    private ActivityService activityService;

    /**
     * 初始化动态服务及 Mapper 模拟对象。
     */
    @BeforeEach
    void setUp() {
        activityMapper = mock(ActivityMapper.class);
        activityService = new ActivityService(activityMapper);
        doAnswer(invocation -> {
            Activity activity = invocation.getArgument(0);
            activity.setId(11L);
            return 1;
        }).when(activityMapper).insert(any(Activity.class));
    }

    /**
     * 验证写入动态时保存对象、动作和摘要。
     */
    @Test
    void shouldRecordActivity() {
        Activity stored = activityService.record(8L, ActivityObjectType.PROJECT, 8L,
                ActivityAction.CREATED, "创建项目「研发罗盘」");

        assertThat(stored.getId()).isEqualTo(11L);
        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).insert(captor.capture());
        Activity activity = captor.getValue();
        assertThat(activity.getProjectId()).isEqualTo(8L);
        assertThat(activity.getObjectType()).isEqualTo(ActivityObjectType.PROJECT);
        assertThat(activity.getObjectId()).isEqualTo(8L);
        assertThat(activity.getAction()).isEqualTo(ActivityAction.CREATED);
        assertThat(activity.getSummary()).isEqualTo("创建项目「研发罗盘」");
        verifyNoMoreInteractions(activityMapper);
    }

    /**
     * 验证任务状态变更动态可以按任务对象写入。
     */
    @Test
    void shouldRecordTaskStatusChange() {
        activityService.record(8L, ActivityObjectType.TASK, 3L,
                ActivityAction.STATUS_CHANGED, "任务状态从 TODO 变为 COMPLETED");

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).insert(captor.capture());
        assertThat(captor.getValue().getObjectType()).isEqualTo(ActivityObjectType.TASK);
        assertThat(captor.getValue().getAction()).isEqualTo(ActivityAction.STATUS_CHANGED);
    }

    /**
     * 验证摘要两侧空白会被去掉。
     */
    @Test
    void shouldTrimSummary() {
        activityService.record(8L, ActivityObjectType.PHASE, 2L, ActivityAction.UPDATED,
                "  更新阶段名称  ");

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).insert(captor.capture());
        assertThat(captor.getValue().getSummary()).isEqualTo("更新阶段名称");
    }

    /**
     * 验证缺少项目时返回校验错误。
     */
    @Test
    void shouldRejectMissingProjectId() {
        assertThatThrownBy(() -> activityService.record(null, ActivityObjectType.PROJECT, 8L,
                ActivityAction.CREATED, "创建项目"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("项目不能为空");
        verifyNoMoreInteractions(activityMapper);
    }

    /**
     * 验证空白摘要时返回校验错误。
     */
    @Test
    void shouldRejectBlankSummary() {
        assertThatThrownBy(() -> activityService.record(8L, ActivityObjectType.PROJECT, 8L,
                ActivityAction.CREATED, "   "))
                .isInstanceOf(BusinessException.class)
                .hasMessage("动态摘要不能为空");
        verifyNoMoreInteractions(activityMapper);
    }

    /**
     * 验证摘要超长时返回校验错误。
     */
    @Test
    void shouldRejectOversizedSummary() {
        String summary = "摘".repeat(ActivityService.SUMMARY_MAX_LENGTH + 1);

        assertThatThrownBy(() -> activityService.record(8L, ActivityObjectType.PROJECT, 8L,
                ActivityAction.CREATED, summary))
                .isInstanceOf(BusinessException.class)
                .hasMessage("动态摘要长度不能超过500个字符");
        verifyNoMoreInteractions(activityMapper);
    }
}
