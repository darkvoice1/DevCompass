package com.darkvoice1.devcompass.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.project.entity.ProjectStatus;
import com.darkvoice1.devcompass.search.dto.SearchItemResponse;
import com.darkvoice1.devcompass.search.dto.SearchItemType;
import com.darkvoice1.devcompass.search.dto.SearchQueryRequest;
import com.darkvoice1.devcompass.search.repository.SearchMapper;
import com.darkvoice1.devcompass.search.service.SearchService;
import com.darkvoice1.devcompass.task.entity.TaskStatus;

/**
 * 验证关键字搜索的类型分流、筛选、排序和摘要截断。
 */
class SearchServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private SearchMapper searchMapper;

    private SearchService searchService;

    /**
     * 初始化搜索服务，时钟固定在上海时区的 2026-09-19。
     */
    @BeforeEach
    void setUp() {
        searchMapper = mock(SearchMapper.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-19T08:00:00+08:00"), ZONE);
        searchService = new SearchService(searchMapper, clock);
    }

    /**
     * 验证未指定类型时会同时查询项目和任务，并按更新时间倒序合并。
     */
    @Test
    void shouldSearchProjectsAndTasksAndSortByUpdatedAt() {
        SearchItemResponse project = item(SearchItemType.PROJECT, 1L, "研发罗盘",
                Instant.parse("2026-09-18T08:00:00Z"), 1L, "研发罗盘");
        SearchItemResponse task = item(SearchItemType.TASK, 2L, "实现搜索接口",
                Instant.parse("2026-09-19T08:00:00Z"), 1L, "研发罗盘");
        when(searchMapper.selectProjects(eq("%接口%"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(project));
        when(searchMapper.selectTasks(eq("%接口%"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(task));

        var response = searchService.search(request("接口", null));

        assertThat(response.getKeyword()).isEqualTo("接口");
        assertThat(response.getItems()).extracting(item -> item.getTitle())
                .containsExactly("实现搜索接口", "研发罗盘");
        verify(searchMapper).selectProjects(eq("%接口%"), isNull(), isNull(), isNull(), isNull());
        verify(searchMapper).selectTasks(eq("%接口%"), isNull(), isNull(), isNull(), isNull());
        verifyNoMoreInteractions(searchMapper);
    }

    /**
     * 验证指定类型为项目时不查询任务。
     */
    @Test
    void shouldSearchProjectsOnlyWhenTypeIsProject() {
        when(searchMapper.selectProjects(eq("%罗盘%"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

        searchService.search(request("罗盘", SearchItemType.PROJECT));

        verify(searchMapper).selectProjects(eq("%罗盘%"), isNull(), isNull(), isNull(), isNull());
        verifyNoMoreInteractions(searchMapper);
    }

    /**
     * 验证指定类型为任务时不查询项目。
     */
    @Test
    void shouldSearchTasksOnlyWhenTypeIsTask() {
        when(searchMapper.selectTasks(eq("%接口%"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

        searchService.search(request("接口", SearchItemType.TASK));

        verify(searchMapper).selectTasks(eq("%接口%"), isNull(), isNull(), isNull(), isNull());
        verifyNoMoreInteractions(searchMapper);
    }

    /**
     * 验证关键字两侧空白会被去掉，特殊字符会被转义。
     */
    @Test
    void shouldTrimKeywordAndEscapeLikeWildcards() {
        when(searchMapper.selectProjects(eq("%100\\%完成%"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of());
        when(searchMapper.selectTasks(eq("%100\\%完成%"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

        var response = searchService.search(request("  100%完成  ", null));

        assertThat(response.getKeyword()).isEqualTo("100%完成");
        verify(searchMapper).selectProjects(eq("%100\\%完成%"), isNull(), isNull(), isNull(), isNull());
        verify(searchMapper).selectTasks(eq("%100\\%完成%"), isNull(), isNull(), isNull(), isNull());
    }

    /**
     * 验证过长描述会被截成短摘要。
     */
    @Test
    void shouldTruncateLongSummary() {
        SearchItemResponse project = item(SearchItemType.PROJECT, 1L, "研发罗盘",
                Instant.parse("2026-09-18T08:00:00Z"), 1L, "研发罗盘");
        project.setSummary("A".repeat(SearchService.SUMMARY_MAX_LENGTH + 10));
        when(searchMapper.selectProjects(eq("%罗盘%"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(project));
        when(searchMapper.selectTasks(eq("%罗盘%"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

        var response = searchService.search(request("罗盘", null));

        assertThat(response.getItems().get(0).getSummary())
                .hasSize(SearchService.SUMMARY_MAX_LENGTH);
    }

    /**
     * 验证没有关键字时仍可按项目筛选，且空白关键字不会生成 LIKE 条件。
     */
    @Test
    void shouldFilterByProjectIdWithoutKeyword() {
        when(searchMapper.selectProjects(isNull(), eq(8L), isNull(), isNull(), isNull()))
                .thenReturn(List.of());
        when(searchMapper.selectTasks(isNull(), eq(8L), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

        SearchQueryRequest request = new SearchQueryRequest();
        request.setProjectId(8L);

        var response = searchService.search(request);

        assertThat(response.getKeyword()).isNull();
        verify(searchMapper).selectProjects(isNull(), eq(8L), isNull(), isNull(), isNull());
        verify(searchMapper).selectTasks(isNull(), eq(8L), isNull(), isNull(), isNull());
        verifyNoMoreInteractions(searchMapper);
    }

    /**
     * 验证 IN_PROGRESS 会同时按项目状态和任务状态筛选。
     */
    @Test
    void shouldApplySharedInProgressStatusToProjectsAndTasks() {
        when(searchMapper.selectProjects(
                eq("%接口%"), isNull(), eq(ProjectStatus.IN_PROGRESS), isNull(), isNull()))
                .thenReturn(List.of());
        when(searchMapper.selectTasks(
                eq("%接口%"), isNull(), eq(TaskStatus.IN_PROGRESS), isNull(), isNull()))
                .thenReturn(List.of());

        SearchQueryRequest request = request("接口", null);
        request.setStatus("IN_PROGRESS");
        searchService.search(request);

        verify(searchMapper).selectProjects(
                eq("%接口%"), isNull(), eq(ProjectStatus.IN_PROGRESS), isNull(), isNull());
        verify(searchMapper).selectTasks(
                eq("%接口%"), isNull(), eq(TaskStatus.IN_PROGRESS), isNull(), isNull());
        verifyNoMoreInteractions(searchMapper);
    }

    /**
     * 验证 TODO 只筛任务，不查项目。
     */
    @Test
    void shouldSkipProjectsWhenStatusIsTaskOnly() {
        when(searchMapper.selectTasks(
                eq("%接口%"), isNull(), eq(TaskStatus.TODO), isNull(), isNull()))
                .thenReturn(List.of());

        SearchQueryRequest request = request("接口", null);
        request.setStatus("TODO");
        searchService.search(request);

        verify(searchMapper).selectTasks(
                eq("%接口%"), isNull(), eq(TaskStatus.TODO), isNull(), isNull());
        verifyNoMoreInteractions(searchMapper);
    }

    /**
     * 验证日期按上海时区换算成当天起止时间，并包含当天。
     */
    @Test
    void shouldConvertDateRangeUsingShanghaiTimezone() {
        Instant updatedFrom = Instant.parse("2026-09-18T16:00:00Z");
        Instant updatedTo = Instant.parse("2026-09-19T16:00:00Z");
        when(searchMapper.selectProjects(eq("%接口%"), isNull(), isNull(), eq(updatedFrom), eq(updatedTo)))
                .thenReturn(List.of());
        when(searchMapper.selectTasks(eq("%接口%"), isNull(), isNull(), eq(updatedFrom), eq(updatedTo)))
                .thenReturn(List.of());

        SearchQueryRequest request = request("接口", null);
        request.setDateFrom(LocalDate.of(2026, 9, 19));
        request.setDateTo(LocalDate.of(2026, 9, 19));
        searchService.search(request);

        verify(searchMapper).selectProjects(
                eq("%接口%"), isNull(), isNull(), eq(updatedFrom), eq(updatedTo));
        verify(searchMapper).selectTasks(
                eq("%接口%"), isNull(), isNull(), eq(updatedFrom), eq(updatedTo));
    }

    /**
     * 验证开始日期晚于结束日期时返回校验错误。
     */
    @Test
    void shouldRejectInvalidDateRange() {
        SearchQueryRequest request = request("接口", null);
        request.setDateFrom(LocalDate.of(2026, 9, 20));
        request.setDateTo(LocalDate.of(2026, 9, 19));

        assertThatThrownBy(() -> searchService.search(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("更新日期范围不合法");
        verifyNoMoreInteractions(searchMapper);
    }

    /**
     * 验证无法识别的状态会返回校验错误。
     */
    @Test
    void shouldRejectUnknownStatus() {
        SearchQueryRequest request = request("接口", null);
        request.setStatus("UNKNOWN");

        assertThatThrownBy(() -> searchService.search(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不支持的状态");
        verifyNoMoreInteractions(searchMapper);
    }

    /**
     * 创建搜索请求。
     */
    private SearchQueryRequest request(String keyword, SearchItemType type) {
        SearchQueryRequest request = new SearchQueryRequest();
        request.setKeyword(keyword);
        request.setType(type);
        return request;
    }

    /**
     * 创建测试用搜索结果。
     */
    private SearchItemResponse item(SearchItemType type, Long id, String title, Instant updatedAt,
            Long projectId, String projectName) {
        SearchItemResponse item = new SearchItemResponse();
        item.setType(type);
        item.setId(id);
        item.setTitle(title);
        item.setUpdatedAt(updatedAt);
        item.setProjectId(projectId);
        item.setProjectName(projectName);
        return item;
    }
}
