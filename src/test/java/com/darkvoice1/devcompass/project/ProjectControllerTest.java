package com.darkvoice1.devcompass.project;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.darkvoice1.devcompass.common.exception.GlobalExceptionHandler;
import com.darkvoice1.devcompass.project.controller.ProjectController;
import com.darkvoice1.devcompass.project.dto.ProjectDetailResponse;
import com.darkvoice1.devcompass.project.entity.ProjectStatus;
import com.darkvoice1.devcompass.project.service.ProjectService;

/**
 * 验证项目创建、详情查询和请求参数校验。
 */
class ProjectControllerTest {

    private ProjectService projectService;

    private MockMvc mockMvc;

    /**
     * 初始化带统一异常处理和参数校验器的 MockMvc。
     */
    @BeforeEach
    void setUp() {
        projectService = mock(ProjectService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectController(projectService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    /**
     * 验证合法创建请求返回项目详情。
     */
    @Test
    void shouldCreateProject() throws Exception {
        when(projectService.createProject(any())).thenReturn(projectResponse());

        mockMvc.perform(post("/api/v1/projects")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "研发罗盘",
                                  "description": "个人研发管理平台",
                                  "status": "IN_PROGRESS",
                                  "targetDate": "2026-12-31",
                                  "techStack": "Java,Spring Boot"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("研发罗盘"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    /**
     * 验证项目名称为空时返回字段级校验错误。
     */
    @Test
    void shouldRejectBlankProjectName() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                        .contentType("application/json")
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(
                        "{\"code\":\"VALIDATION_ERROR\",\"message\":\"请求参数校验失败\","
                                + "\"data\":{\"name\":\"项目名称不能为空\"}}"));
    }

    /**
     * 验证技术栈超过长度限制时返回字段级校验错误。
     */
    @Test
    void shouldRejectTooLongTechStack() throws Exception {
        String techStack = "a".repeat(501);

        mockMvc.perform(post("/api/v1/projects")
                        .contentType("application/json")
                        .content("{\"name\":\"研发罗盘\",\"techStack\":\"" + techStack + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.techStack").value("技术栈长度不能超过500个字符"));
    }

    /**
     * 验证可以按项目主键查询详情。
     */
    @Test
    void shouldGetProjectDetail() throws Exception {
        when(projectService.getProjectDetail(1L)).thenReturn(projectResponse());

        mockMvc.perform(get("/api/v1/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.targetDate").value("2026-12-31"));
    }

    /**
     * 验证合法编辑请求返回更新后的项目详情。
     */
    @Test
    void shouldUpdateProject() throws Exception {
        ProjectDetailResponse response = projectResponse();
        response.setName("更新后的研发罗盘");
        response.setStatus(ProjectStatus.COMPLETED);
        when(projectService.updateProject(any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/projects/1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "更新后的研发罗盘",
                                  "description": "更新后的项目描述",
                                  "status": "COMPLETED",
                                  "targetDate": "2027-01-31",
                                  "techStack": "Java,Spring Boot,PostgreSQL"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("更新后的研发罗盘"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    /**
     * 验证编辑请求缺少项目名称时返回校验错误。
     */
    @Test
    void shouldRejectUpdateWithoutProjectName() throws Exception {
        mockMvc.perform(put("/api/v1/projects/1")
                        .contentType("application/json")
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.name").value("项目名称不能为空"));
    }

    /**
     * 创建测试用项目详情响应。
     */
    private ProjectDetailResponse projectResponse() {
        ProjectDetailResponse response = new ProjectDetailResponse();
        response.setId(1L);
        response.setName("研发罗盘");
        response.setDescription("个人研发管理平台");
        response.setStatus(ProjectStatus.IN_PROGRESS);
        response.setTargetDate(LocalDate.of(2026, 12, 31));
        response.setTechStack("Java,Spring Boot");
        return response;
    }
}
