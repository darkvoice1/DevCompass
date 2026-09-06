package com.darkvoice1.devcompass.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.darkvoice1.devcompass.common.exception.GlobalExceptionHandler;
import com.darkvoice1.devcompass.task.controller.TaskController;
import com.darkvoice1.devcompass.task.dto.TaskDetailResponse;
import com.darkvoice1.devcompass.task.entity.TaskPriority;
import com.darkvoice1.devcompass.task.entity.TaskStatus;
import com.darkvoice1.devcompass.task.service.TaskService;

/**
 * 验证任务接口和参数校验。
 */
class TaskControllerTest {

    private TaskService taskService;
    private MockMvc mockMvc;

    /**
     * 初始化带参数校验的 MockMvc。
     */
    @BeforeEach
    void setUp() {
        taskService = mock(TaskService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new TaskController(taskService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    /**
     * 验证合法请求可以创建任务。
     */
    @Test
    void shouldCreateTask() throws Exception {
        when(taskService.createTask(any())).thenReturn(taskResponse());

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType("application/json")
                        .content("{\"projectId\":1,\"title\":\"实现任务接口\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.title").value("实现任务接口"));
    }

    /**
     * 验证任务标题为空时返回参数错误。
     */
    @Test
    void shouldRejectBlankTaskTitle() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType("application/json")
                        .content("{\"projectId\":1,\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.title").value("任务标题不能为空"));
    }

    /**
     * 验证任务编辑接口。
     */
    @Test
    void shouldUpdateTask() throws Exception {
        when(taskService.updateTask(any(), any())).thenReturn(taskResponse());

        mockMvc.perform(put("/api/v1/tasks/10")
                        .contentType("application/json")
                        .content("{\"title\":\"更新任务\",\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    private TaskDetailResponse taskResponse() {
        TaskDetailResponse response = new TaskDetailResponse();
        response.setId(10L);
        response.setProjectId(1L);
        response.setTitle("实现任务接口");
        response.setStatus(TaskStatus.TODO);
        response.setPriority(TaskPriority.MEDIUM);
        return response;
    }
}
