package com.darkvoice1.devcompass.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.darkvoice1.devcompass.search.dto.SearchItemResponse;
import com.darkvoice1.devcompass.search.dto.SearchItemType;
import com.darkvoice1.devcompass.search.dto.SearchQueryRequest;
import com.darkvoice1.devcompass.search.repository.SearchMapper;
import com.darkvoice1.devcompass.search.service.SearchService;

/**
 * 验证关键字搜索的类型分流、排序和摘要截断。
 */
class SearchServiceTest {

    private SearchMapper searchMapper;

    private SearchService searchService;

    /**
     * 初始化搜索服务及 Mapper 模拟对象。
     */
    @BeforeEach
    void setUp() {
        searchMapper = mock(SearchMapper.class);
        searchService = new SearchService(searchMapper);
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
        when(searchMapper.selectProjects("%接口%")).thenReturn(List.of(project));
        when(searchMapper.selectTasks("%接口%")).thenReturn(List.of(task));

        var response = searchService.search(request("接口", null));

        assertThat(response.getKeyword()).isEqualTo("接口");
        assertThat(response.getItems()).extracting(item -> item.getTitle())
                .containsExactly("实现搜索接口", "研发罗盘");
        verify(searchMapper).selectProjects("%接口%");
        verify(searchMapper).selectTasks("%接口%");
        verifyNoMoreInteractions(searchMapper);
    }

    /**
     * 验证指定类型为项目时不查询任务。
     */
    @Test
    void shouldSearchProjectsOnlyWhenTypeIsProject() {
        when(searchMapper.selectProjects("%罗盘%")).thenReturn(List.of());

        searchService.search(request("罗盘", SearchItemType.PROJECT));

        verify(searchMapper).selectProjects("%罗盘%");
        verifyNoMoreInteractions(searchMapper);
    }

    /**
     * 验证指定类型为任务时不查询项目。
     */
    @Test
    void shouldSearchTasksOnlyWhenTypeIsTask() {
        when(searchMapper.selectTasks("%接口%")).thenReturn(List.of());

        searchService.search(request("接口", SearchItemType.TASK));

        verify(searchMapper).selectTasks("%接口%");
        verifyNoMoreInteractions(searchMapper);
    }

    /**
     * 验证关键字两侧空白会被去掉，特殊字符会被转义。
     */
    @Test
    void shouldTrimKeywordAndEscapeLikeWildcards() {
        when(searchMapper.selectProjects("%100\\%完成%")).thenReturn(List.of());
        when(searchMapper.selectTasks("%100\\%完成%")).thenReturn(List.of());

        var response = searchService.search(request("  100%完成  ", null));

        assertThat(response.getKeyword()).isEqualTo("100%完成");
        verify(searchMapper).selectProjects("%100\\%完成%");
        verify(searchMapper).selectTasks("%100\\%完成%");
    }

    /**
     * 验证过长描述会被截成短摘要。
     */
    @Test
    void shouldTruncateLongSummary() {
        SearchItemResponse project = item(SearchItemType.PROJECT, 1L, "研发罗盘",
                Instant.parse("2026-09-18T08:00:00Z"), 1L, "研发罗盘");
        project.setSummary("A".repeat(SearchService.SUMMARY_MAX_LENGTH + 10));
        when(searchMapper.selectProjects("%罗盘%")).thenReturn(List.of(project));
        when(searchMapper.selectTasks("%罗盘%")).thenReturn(List.of());

        var response = searchService.search(request("罗盘", null));

        assertThat(response.getItems().get(0).getSummary())
                .hasSize(SearchService.SUMMARY_MAX_LENGTH);
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
