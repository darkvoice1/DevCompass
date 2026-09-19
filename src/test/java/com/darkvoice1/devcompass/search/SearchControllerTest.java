package com.darkvoice1.devcompass.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.darkvoice1.devcompass.common.exception.GlobalExceptionHandler;
import com.darkvoice1.devcompass.search.controller.SearchController;
import com.darkvoice1.devcompass.search.dto.SearchItemResponse;
import com.darkvoice1.devcompass.search.dto.SearchItemType;
import com.darkvoice1.devcompass.search.dto.SearchQueryRequest;
import com.darkvoice1.devcompass.search.dto.SearchResponse;
import com.darkvoice1.devcompass.search.service.SearchService;

/**
 * 验证全局搜索接口。
 */
class SearchControllerTest {

    private SearchService searchService;

    private MockMvc mockMvc;

    /**
     * 初始化搜索控制器测试环境。
     */
    @BeforeEach
    void setUp() {
        searchService = mock(SearchService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new SearchController(searchService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    /**
     * 验证关键字搜索返回类型、标题和跳转字段。
     */
    @Test
    void shouldSearchByKeyword() throws Exception {
        when(searchService.search(any())).thenReturn(searchResponse());

        mockMvc.perform(get("/api/v1/search").param("keyword", "接口"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.keyword").value("接口"))
                .andExpect(jsonPath("$.data.items[0].type").value("TASK"))
                .andExpect(jsonPath("$.data.items[0].id").value(2))
                .andExpect(jsonPath("$.data.items[0].title").value("实现搜索接口"))
                .andExpect(jsonPath("$.data.items[0].projectId").value(1))
                .andExpect(jsonPath("$.data.items[0].projectName").value("研发罗盘"));
    }

    /**
     * 验证可选类型可以绑定为查询参数。
     */
    @Test
    void shouldBindOptionalSearchType() throws Exception {
        when(searchService.search(any())).thenReturn(searchResponse());

        mockMvc.perform(get("/api/v1/search").param("keyword", "接口").param("type", "TASK"))
                .andExpect(status().isOk());

        ArgumentCaptor<SearchQueryRequest> captor = ArgumentCaptor.forClass(SearchQueryRequest.class);
        verify(searchService).search(captor.capture());
        assertThat(captor.getValue().getKeyword()).isEqualTo("接口");
        assertThat(captor.getValue().getType()).isEqualTo(SearchItemType.TASK);
    }

    /**
     * 验证缺少关键字时返回参数校验错误。
     */
    @Test
    void shouldRejectBlankKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("keyword", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.keyword").value("搜索关键字不能为空"));
    }

    /**
     * 创建测试用搜索响应。
     */
    private SearchResponse searchResponse() {
        SearchItemResponse item = new SearchItemResponse();
        item.setType(SearchItemType.TASK);
        item.setId(2L);
        item.setTitle("实现搜索接口");
        item.setSummary("完成统一搜索");
        item.setUpdatedAt(Instant.parse("2026-09-19T08:00:00Z"));
        item.setProjectId(1L);
        item.setProjectName("研发罗盘");

        SearchResponse response = new SearchResponse();
        response.setKeyword("接口");
        response.setItems(List.of(item));
        return response;
    }
}
