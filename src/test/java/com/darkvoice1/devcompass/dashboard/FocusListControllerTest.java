package com.darkvoice1.devcompass.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.darkvoice1.devcompass.common.exception.GlobalExceptionHandler;
import com.darkvoice1.devcompass.dashboard.controller.FocusListController;
import com.darkvoice1.devcompass.dashboard.dto.FocusListItemKind;
import com.darkvoice1.devcompass.dashboard.dto.FocusListItemResponse;
import com.darkvoice1.devcompass.dashboard.dto.FocusListQueryRequest;
import com.darkvoice1.devcompass.dashboard.dto.FocusListResponse;
import com.darkvoice1.devcompass.dashboard.dto.FocusListType;
import com.darkvoice1.devcompass.dashboard.service.FocusListService;
import com.darkvoice1.devcompass.task.entity.TaskStatus;

/**
 * 验证首页焦点清单查询接口。
 */
class FocusListControllerTest {

    private FocusListService focusListService;

    private MockMvc mockMvc;

    /**
     * 初始化焦点清单控制器测试环境。
     */
    @BeforeEach
    void setUp() {
        focusListService = mock(FocusListService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new FocusListController(focusListService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    /**
     * 验证本周清单接口返回日期范围、任务和跳转字段。
     */
    @Test
    void shouldGetThisWeekFocusList() throws Exception {
        when(focusListService.getFocusList(any())).thenReturn(thisWeekResponse());

        mockMvc.perform(get("/api/v1/dashboard/focus-lists").param("type", "THIS_WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.type").value("THIS_WEEK"))
                .andExpect(jsonPath("$.data.fromDate").value("2026-09-14"))
                .andExpect(jsonPath("$.data.toDate").value("2026-09-20"))
                .andExpect(jsonPath("$.data.items[0].itemKind").value("TASK"))
                .andExpect(jsonPath("$.data.items[0].taskId").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("实现本周清单"))
                .andExpect(jsonPath("$.data.items[0].status").value("TODO"))
                .andExpect(jsonPath("$.data.items[0].dueDate").value("2026-09-16"))
                .andExpect(jsonPath("$.data.items[0].projectId").value(10))
                .andExpect(jsonPath("$.data.items[0].projectName").value("研发罗盘"));
    }

    /**
     * 验证清单类型可以绑定为查询参数。
     */
    @Test
    void shouldBindFocusListType() throws Exception {
        when(focusListService.getFocusList(any())).thenReturn(thisWeekResponse());

        mockMvc.perform(get("/api/v1/dashboard/focus-lists").param("type", "THIS_WEEK"))
                .andExpect(status().isOk());

        ArgumentCaptor<FocusListQueryRequest> captor =
                ArgumentCaptor.forClass(FocusListQueryRequest.class);
        verify(focusListService).getFocusList(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(FocusListType.THIS_WEEK);
    }

    /**
     * 验证逾期和即将到期类型可以绑定为查询参数。
     */
    @Test
    void shouldBindOverdueAndDueSoonTypes() throws Exception {
        when(focusListService.getFocusList(any())).thenReturn(thisWeekResponse());

        mockMvc.perform(get("/api/v1/dashboard/focus-lists").param("type", "OVERDUE"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/dashboard/focus-lists").param("type", "DUE_SOON"))
                .andExpect(status().isOk());
    }

    /**
     * 验证逾期清单可以同时返回任务和延期项目。
     */
    @Test
    void shouldGetOverdueFocusListWithTaskAndProject() throws Exception {
        when(focusListService.getFocusList(any())).thenReturn(overdueResponse());

        mockMvc.perform(get("/api/v1/dashboard/focus-lists").param("type", "OVERDUE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("OVERDUE"))
                .andExpect(jsonPath("$.data.fromDate").value(nullValue()))
                .andExpect(jsonPath("$.data.toDate").value("2026-09-15"))
                .andExpect(jsonPath("$.data.items[0].itemKind").value("TASK"))
                .andExpect(jsonPath("$.data.items[0].taskId").value(1))
                .andExpect(jsonPath("$.data.items[1].itemKind").value("PROJECT"))
                .andExpect(jsonPath("$.data.items[1].taskId").value(nullValue()))
                .andExpect(jsonPath("$.data.items[1].projectId").value(20))
                .andExpect(jsonPath("$.data.items[1].title").value("延期项目"));
    }

    /**
     * 验证缺少清单类型时返回参数校验错误。
     */
    @Test
    void shouldRejectMissingFocusListType() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/focus-lists"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.type").value("清单类型不能为空"));
    }

    /**
     * 创建测试用本周清单响应。
     */
    private FocusListResponse thisWeekResponse() {
        FocusListItemResponse item = new FocusListItemResponse();
        item.setItemKind(FocusListItemKind.TASK);
        item.setTaskId(1L);
        item.setTitle("实现本周清单");
        item.setStatus(TaskStatus.TODO);
        item.setDueDate(LocalDate.of(2026, 9, 16));
        item.setProjectId(10L);
        item.setProjectName("研发罗盘");

        FocusListResponse response = new FocusListResponse();
        response.setType(FocusListType.THIS_WEEK);
        response.setFromDate(LocalDate.of(2026, 9, 14));
        response.setToDate(LocalDate.of(2026, 9, 20));
        response.setItems(List.of(item));
        return response;
    }

    /**
     * 创建测试用逾期清单响应。
     */
    private FocusListResponse overdueResponse() {
        FocusListItemResponse taskItem = new FocusListItemResponse();
        taskItem.setItemKind(FocusListItemKind.TASK);
        taskItem.setTaskId(1L);
        taskItem.setTitle("逾期任务");
        taskItem.setStatus(TaskStatus.TODO);
        taskItem.setDueDate(LocalDate.of(2026, 9, 15));
        taskItem.setProjectId(10L);
        taskItem.setProjectName("研发罗盘");

        FocusListItemResponse projectItem = new FocusListItemResponse();
        projectItem.setItemKind(FocusListItemKind.PROJECT);
        projectItem.setTitle("延期项目");
        projectItem.setDueDate(LocalDate.of(2026, 9, 1));
        projectItem.setProjectId(20L);
        projectItem.setProjectName("延期项目");

        FocusListResponse response = new FocusListResponse();
        response.setType(FocusListType.OVERDUE);
        response.setToDate(LocalDate.of(2026, 9, 15));
        response.setItems(List.of(taskItem, projectItem));
        return response;
    }
}
