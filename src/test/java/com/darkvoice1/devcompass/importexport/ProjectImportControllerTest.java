package com.darkvoice1.devcompass.importexport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.common.exception.GlobalExceptionHandler;
import com.darkvoice1.devcompass.importexport.controller.ProjectImportController;
import com.darkvoice1.devcompass.importexport.dto.ProjectImportResponse;
import com.darkvoice1.devcompass.importexport.service.ProjectImportService;

/**
 * 验证项目 JSON 导入接口。
 */
class ProjectImportControllerTest {

    private ProjectImportService projectImportService;

    private MockMvc mockMvc;

    /**
     * 初始化带统一异常处理的 MockMvc。
     */
    @BeforeEach
    void setUp() {
        projectImportService = mock(ProjectImportService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectImportController(projectImportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证导入接口返回新项目编号和跳过的附件数量。
     */
    @Test
    void shouldImportProject() throws Exception {
        ProjectImportResponse response = new ProjectImportResponse();
        response.setProjectId(100L);
        response.setProjectName("研发罗盘");
        response.setSkippedAttachmentCount(1);
        when(projectImportService.importProject(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/projects/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formatVersion\":1,\"project\":{\"name\":\"研发罗盘\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.projectId").value(100))
                .andExpect(jsonPath("$.data.projectName").value("研发罗盘"))
                .andExpect(jsonPath("$.data.skippedAttachmentCount").value(1));
    }

    /**
     * 验证同名项目被拒绝时返回业务错误。
     */
    @Test
    void shouldRejectDuplicateProject() throws Exception {
        when(projectImportService.importProject(any()))
                .thenThrow(new BusinessException(ErrorCode.BUSINESS_ERROR, "已存在同名项目，不能覆盖"));

        mockMvc.perform(post("/api/v1/projects/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formatVersion\":1,\"project\":{\"name\":\"研发罗盘\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("已存在同名项目，不能覆盖"));
    }
}
