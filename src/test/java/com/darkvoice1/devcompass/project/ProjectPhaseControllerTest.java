package com.darkvoice1.devcompass.project;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.darkvoice1.devcompass.common.exception.GlobalExceptionHandler;
import com.darkvoice1.devcompass.project.controller.ProjectPhaseController;
import com.darkvoice1.devcompass.project.dto.ProjectPhaseResponse;
import com.darkvoice1.devcompass.project.service.ProjectPhaseService;

/**
 * 验证项目阶段接口和请求参数校验。
 */
class ProjectPhaseControllerTest {

    private ProjectPhaseService projectPhaseService;

    private MockMvc mockMvc;

    /**
     * 初始化带统一异常处理和参数校验器的 MockMvc。
     */
    @BeforeEach
    void setUp() {
        projectPhaseService = mock(ProjectPhaseService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectPhaseController(projectPhaseService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    /**
     * 验证可以在项目中创建阶段。
     */
    @Test
    void shouldCreateProjectPhase() throws Exception {
        when(projectPhaseService.createProjectPhase(any(), any())).thenReturn(phaseResponse());

        mockMvc.perform(post("/api/v1/projects/1/phases")
                        .contentType("application/json")
                        .content("{\"name\":\"需求分析\",\"description\":\"梳理需求\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.sortOrder").value(0));
    }

    /**
     * 验证阶段名称为空时返回参数校验错误。
     */
    @Test
    void shouldRejectBlankPhaseName() throws Exception {
        mockMvc.perform(post("/api/v1/projects/1/phases")
                        .contentType("application/json")
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.name").value("阶段名称不能为空"));
    }

    /**
     * 验证可以查询项目阶段列表。
     */
    @Test
    void shouldGetProjectPhases() throws Exception {
        when(projectPhaseService.getProjectPhases(1L)).thenReturn(List.of(phaseResponse()));

        mockMvc.perform(get("/api/v1/projects/1/phases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data[0].name").value("需求分析"));
    }

    /**
     * 验证可以编辑项目阶段。
     */
    @Test
    void shouldUpdateProjectPhase() throws Exception {
        ProjectPhaseResponse response = phaseResponse();
        response.setName("开发实现");
        when(projectPhaseService.updateProjectPhase(any(), any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/projects/1/phases/1")
                        .contentType("application/json")
                        .content("{\"name\":\"开发实现\",\"description\":\"完成编码\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("开发实现"));
    }

    /**
     * 验证可以调整项目阶段排序。
     */
    @Test
    void shouldUpdateProjectPhaseSortOrder() throws Exception {
        when(projectPhaseService.updateProjectPhaseSortOrder(any(), any(), any()))
                .thenReturn(phaseResponse());

        mockMvc.perform(put("/api/v1/projects/1/phases/1/sort-order")
                        .contentType("application/json")
                        .content("{\"sortOrder\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sortOrder").value(0));
    }

    /**
     * 验证排序序号为负数时返回参数校验错误。
     */
    @Test
    void shouldRejectNegativeSortOrder() throws Exception {
        mockMvc.perform(put("/api/v1/projects/1/phases/1/sort-order")
                        .contentType("application/json")
                        .content("{\"sortOrder\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.sortOrder").value("排序序号不能小于0"));
    }

    /**
     * 创建测试用的项目阶段响应。
     */
    private ProjectPhaseResponse phaseResponse() {
        ProjectPhaseResponse response = new ProjectPhaseResponse();
        response.setId(1L);
        response.setProjectId(1L);
        response.setName("需求分析");
        response.setDescription("梳理需求");
        response.setSortOrder(0);
        return response;
    }
}
