package com.darkvoice1.devcompass.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
                        .content("{\"projectId\":1,\"phaseId\":2,\"title\":\"实现任务接口\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.title").value("实现任务接口"))
                .andExpect(jsonPath("$.data.phaseName").value("开发实现"));
    }

    /**
     * 验证任务标题为空时返回参数错误。
     */
    @Test
    void shouldRejectBlankTaskTitle() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType("application/json")
                        .content("{\"projectId\":1,\"phaseId\":2,\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.title").value("任务标题不能为空"));
    }

    /**
     * 验证创建任务时必须传入所属阶段。
     */
    @Test
    void shouldRejectTaskWithoutPhase() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType("application/json")
                        .content("{\"projectId\":1,\"title\":\"实现任务接口\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.phaseId").value("任务阶段不能为空"));
    }

    /**
     * 验证任务编辑接口。
     */
    @Test
    void shouldUpdateTask() throws Exception {
        when(taskService.updateTask(any(), any())).thenReturn(taskResponse());

        mockMvc.perform(put("/api/v1/tasks/10")
                        .contentType("application/json")
                        .content("{\"title\":\"更新任务\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    /**
     * 验证任务状态变更接口。
     */
    @Test
    void shouldChangeTaskStatus() throws Exception {
        when(taskService.changeTaskStatus(any(), any())).thenReturn(taskResponse());

        mockMvc.perform(patch("/api/v1/tasks/10/status")
                        .contentType("application/json")
                        .content("{\"targetStatus\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.id").value(10));
    }

    /**
     * 验证状态变更请求必须提供目标状态。
     */
    @Test
    void shouldRejectStatusChangeWithoutTarget() throws Exception {
        mockMvc.perform(patch("/api/v1/tasks/10/status")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.targetStatus").value("目标状态不能为空"));
    }

    /**
     * 验证任务查询接口支持筛选参数。
     */
    @Test
    void shouldQueryTasks() throws Exception {
        when(taskService.queryTasks(any(), any(), any(), any())).thenReturn(java.util.List.of(taskResponse()));

        mockMvc.perform(get("/api/v1/tasks")
                        .param("projectId", "1")
                        .param("status", "TODO")
                        .param("priority", "HIGH")
                        .param("keyword", "接口"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(10));
    }

    /**
     * 验证任务软删除接口。
     */
    @Test
    void shouldDeleteTask() throws Exception {
        mockMvc.perform(delete("/api/v1/tasks/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    /**
     * 验证任务恢复软删除接口。
     */
    @Test
    void shouldRestoreDeletedTask() throws Exception {
        mockMvc.perform(post("/api/v1/tasks/10/restore-deleted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    /**
     * 创建测试用任务详情响应。
     *
     * @return 测试用任务详情响应
     */
    private TaskDetailResponse taskResponse() {
        TaskDetailResponse response = new TaskDetailResponse();
        response.setId(10L);
        response.setProjectId(1L);
        response.setPhaseId(2L);
        response.setPhaseName("开发实现");
        response.setTitle("实现任务接口");
        response.setStatus(TaskStatus.TODO);
        response.setPriority(TaskPriority.MEDIUM);
        return response;
    }
}
