package com.darkvoice1.devcompass.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.darkvoice1.devcompass.task.entity.TaskPriority;
import com.darkvoice1.devcompass.task.entity.TaskStatus;
import com.darkvoice1.devcompass.timeline.controller.TimelineController;
import com.darkvoice1.devcompass.timeline.dto.TimelineEventResponse;
import com.darkvoice1.devcompass.timeline.dto.TimelineEventType;
import com.darkvoice1.devcompass.timeline.dto.TimelineQueryRequest;
import com.darkvoice1.devcompass.timeline.dto.TimelineResponse;
import com.darkvoice1.devcompass.timeline.service.TimelineService;

/**
 * 验证时间线查询接口。
 */
class TimelineControllerTest {

    private TimelineService timelineService;

    private MockMvc mockMvc;

    /**
     * 初始化时间线控制器测试环境。
     */
    @BeforeEach
    void setUp() {
        timelineService = mock(TimelineService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new TimelineController(timelineService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    /**
     * 验证按日期范围返回任务事件和完成标记。
     */
    @Test
    void shouldGetTimelineTaskEvents() throws Exception {
        when(timelineService.getTimeline(any())).thenReturn(timelineResponse());

        mockMvc.perform(get("/api/v1/timeline")
                        .param("fromDate", "2026-09-01")
                        .param("toDate", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.fromDate").value("2026-09-01"))
                .andExpect(jsonPath("$.data.toDate").value("2026-09-30"))
                .andExpect(jsonPath("$.data.items[0].type").value("TASK"))
                .andExpect(jsonPath("$.data.items[0].id").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("实现时间线"))
                .andExpect(jsonPath("$.data.items[0].date").value("2026-09-16"))
                .andExpect(jsonPath("$.data.items[0].projectId").value(10))
                .andExpect(jsonPath("$.data.items[0].status").value("TODO"))
                .andExpect(jsonPath("$.data.items[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.data.items[0].completed").value(false))
                .andExpect(jsonPath("$.data.items[1].type").value("PROJECT"))
                .andExpect(jsonPath("$.data.items[1].id").value(8))
                .andExpect(jsonPath("$.data.items[1].date").value("2026-09-30"))
                .andExpect(jsonPath("$.data.items[1].completed").value(false));
    }

    /**
     * 验证开始日期和结束日期可以绑定为查询参数。
     */
    @Test
    void shouldBindDateRange() throws Exception {
        when(timelineService.getTimeline(any())).thenReturn(timelineResponse());

        mockMvc.perform(get("/api/v1/timeline")
                        .param("fromDate", "2026-09-01")
                        .param("toDate", "2026-09-30"))
                .andExpect(status().isOk());

        ArgumentCaptor<TimelineQueryRequest> captor =
                ArgumentCaptor.forClass(TimelineQueryRequest.class);
        verify(timelineService).getTimeline(captor.capture());
        assertThat(captor.getValue().getFromDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(captor.getValue().getToDate()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    /**
     * 验证缺少开始日期时返回参数校验错误。
     */
    @Test
    void shouldRejectMissingFromDate() throws Exception {
        mockMvc.perform(get("/api/v1/timeline").param("toDate", "2026-09-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.fromDate").value("开始日期不能为空"));
    }

    /**
     * 创建测试用时间线响应。
     */
    private TimelineResponse timelineResponse() {
        TimelineEventResponse event = new TimelineEventResponse();
        event.setType(TimelineEventType.TASK);
        event.setId(1L);
        event.setTitle("实现时间线");
        event.setDate(LocalDate.of(2026, 9, 16));
        event.setProjectId(10L);
        event.setProjectName("研发罗盘");
        event.setStatus(TaskStatus.TODO);
        event.setPriority(TaskPriority.HIGH);
        event.setCompleted(false);

        TimelineEventResponse project = new TimelineEventResponse();
        project.setType(TimelineEventType.PROJECT);
        project.setId(8L);
        project.setTitle("研发罗盘");
        project.setDate(LocalDate.of(2026, 9, 30));
        project.setProjectId(8L);
        project.setProjectName("研发罗盘");
        project.setCompleted(false);

        TimelineResponse response = new TimelineResponse();
        response.setFromDate(LocalDate.of(2026, 9, 1));
        response.setToDate(LocalDate.of(2026, 9, 30));
        response.setItems(List.of(event, project));
        return response;
    }
}
