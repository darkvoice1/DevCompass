package com.darkvoice1.devcompass.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.darkvoice1.devcompass.activity.dto.ActivityQueryRequest;
import com.darkvoice1.devcompass.activity.entity.Activity;
import com.darkvoice1.devcompass.activity.entity.ActivityAction;
import com.darkvoice1.devcompass.activity.entity.ActivityObjectType;
import com.darkvoice1.devcompass.activity.repository.ActivityMapper;
import com.darkvoice1.devcompass.activity.service.ActivityService;
import com.darkvoice1.devcompass.common.exception.BusinessException;

/**
 * 验证动态写入和分页查询。
 */
class ActivityServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private ActivityMapper activityMapper;

    private ActivityService activityService;

    /**
     * 初始化动态服务及 Mapper 模拟对象。
     */
    @BeforeEach
    void setUp() {
        activityMapper = mock(ActivityMapper.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-21T08:00:00+08:00"), ZONE);
        activityService = new ActivityService(activityMapper, clock);
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

    /**
     * 验证按项目查询动态时返回分页数据和摘要。
     */
    @Test
    void shouldQueryActivitiesByProject() {
        stubSelectPage(activity(21L, ActivityObjectType.TASK, ActivityAction.STATUS_CHANGED,
                "任务状态从 TODO 变为 COMPLETED"));

        var response = activityService.queryActivities(queryRequest(8L, null, null, null, 1L, 20));

        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getPageSize()).isEqualTo(20);
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getRecords()).hasSize(1);
        assertThat(response.getRecords().getFirst().getObjectType())
                .isEqualTo(ActivityObjectType.TASK);
        assertThat(response.getRecords().getFirst().getSummary())
                .isEqualTo("任务状态从 TODO 变为 COMPLETED");
        ArgumentCaptor<QueryWrapper<Activity>> captor = ArgumentCaptor.captor();
        verify(activityMapper).selectPage(any(), captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("project_id");
        assertThat(captor.getValue().getSqlSegment()).contains("ORDER BY created_at DESC");
    }

    /**
     * 验证可以按对象类型筛选动态。
     */
    @Test
    void shouldFilterActivitiesByObjectType() {
        stubSelectPage(activity(21L, ActivityObjectType.PHASE, ActivityAction.CREATED,
                "创建阶段「开发实现」"));

        activityService.queryActivities(queryRequest(8L, ActivityObjectType.PHASE, null, null, 1L, 20));

        ArgumentCaptor<QueryWrapper<Activity>> captor = ArgumentCaptor.captor();
        verify(activityMapper).selectPage(any(), captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("object_type");
        assertThat(captor.getValue().getParamNameValuePairs().values())
                .contains(8L, ActivityObjectType.PHASE);
    }

    /**
     * 验证日期范围按上海时区包含当天。
     */
    @Test
    void shouldFilterActivitiesByInclusiveShanghaiDate() {
        stubSelectPage(activity(21L, ActivityObjectType.PROJECT, ActivityAction.CREATED,
                "创建项目「研发罗盘」"));

        activityService.queryActivities(queryRequest(8L, null,
                LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 21), 1L, 20));

        ArgumentCaptor<QueryWrapper<Activity>> captor = ArgumentCaptor.captor();
        verify(activityMapper).selectPage(any(), captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("created_at");
        assertThat(captor.getValue().getParamNameValuePairs().values())
                .contains(Instant.parse("2026-09-20T16:00:00Z"), Instant.parse("2026-09-21T16:00:00Z"));
    }

    /**
     * 验证开始日期晚于结束日期时返回校验错误。
     */
    @Test
    void shouldRejectInvalidDateRange() {
        assertThatThrownBy(() -> activityService.queryActivities(queryRequest(8L, null,
                LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 21), 1L, 20)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("日期范围不合法");
        verifyNoMoreInteractions(activityMapper);
    }

    /**
     * 验证每页数量超过上限时返回校验错误。
     */
    @Test
    void shouldRejectInvalidPageSize() {
        assertThatThrownBy(() -> activityService.queryActivities(
                queryRequest(8L, null, null, null, 1L, 101)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("分页参数不合法");
        verifyNoMoreInteractions(activityMapper);
    }

    /**
     * 模拟分页查询返回一条动态。
     */
    private void stubSelectPage(Activity activity) {
        when(activityMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<Activity> page = invocation.getArgument(0);
            page.setRecords(List.of(activity));
            page.setTotal(1);
            return page;
        });
    }

    /**
     * 创建查询请求。
     */
    private ActivityQueryRequest queryRequest(Long projectId, ActivityObjectType objectType,
            LocalDate dateFrom, LocalDate dateTo, Long page, Integer pageSize) {
        ActivityQueryRequest request = new ActivityQueryRequest();
        request.setProjectId(projectId);
        request.setObjectType(objectType);
        request.setDateFrom(dateFrom);
        request.setDateTo(dateTo);
        request.setPage(page);
        request.setPageSize(pageSize);
        return request;
    }

    /**
     * 创建用于查询结果的动态实体。
     */
    private Activity activity(Long id, ActivityObjectType objectType, ActivityAction action,
            String summary) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setProjectId(8L);
        activity.setObjectType(objectType);
        activity.setObjectId(3L);
        activity.setAction(action);
        activity.setSummary(summary);
        activity.setCreatedAt(Instant.parse("2026-09-21T02:00:00Z"));
        return activity;
    }
}
