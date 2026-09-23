package com.darkvoice1.devcompass.importexport;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.common.exception.GlobalExceptionHandler;
import com.darkvoice1.devcompass.importexport.controller.ProjectExportController;
import com.darkvoice1.devcompass.importexport.dto.ProjectExportFile;
import com.darkvoice1.devcompass.importexport.service.ProjectExportService;

/**
 * 验证项目 JSON 导出接口。
 */
class ProjectExportControllerTest {

    private ProjectExportService projectExportService;

    private MockMvc mockMvc;

    /**
     * 初始化带统一异常处理的 MockMvc。
     */
    @BeforeEach
    void setUp() {
        projectExportService = mock(ProjectExportService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectExportController(projectExportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证导出接口下载 JSON 文件，并且附件没有存储编号。
     */
    @Test
    void shouldDownloadProjectExportFile() throws Exception {
        when(projectExportService.exportProject(8L)).thenReturn(exportFile());

        mockMvc.perform(get("/api/v1/projects/8/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"project-8.json\""))
                .andExpect(jsonPath("$.formatVersion").value(1))
                .andExpect(jsonPath("$.project.name").value("研发罗盘"))
                .andExpect(jsonPath("$.project.tags").value("后端,Java"))
                .andExpect(jsonPath("$.phases[0].name").value("开发实现"))
                .andExpect(jsonPath("$.tasks[0].title").value("导出项目"))
                .andExpect(jsonPath("$.attachments[0].originalFileName").value("设计图.png"))
                .andExpect(jsonPath("$.attachments[0].storageKey").doesNotExist())
                .andExpect(jsonPath("$.project.deletedAt").doesNotExist());
    }

    /**
     * 验证项目不存在时返回业务错误。
     */
    @Test
    void shouldRejectMissingProject() throws Exception {
        when(projectExportService.exportProject(8L))
                .thenThrow(new BusinessException(ErrorCode.BUSINESS_ERROR, "项目不存在"));

        mockMvc.perform(get("/api/v1/projects/8/export"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("项目不存在"));
    }

    private ProjectExportFile exportFile() {
        ProjectExportFile file = new ProjectExportFile();
        file.setFormatVersion(ProjectExportFile.FORMAT_VERSION);
        file.setExportedAt(Instant.parse("2026-09-22T02:00:00Z"));
        ProjectExportFile.ProjectItem project = new ProjectExportFile.ProjectItem();
        project.setId(8L);
        project.setName("研发罗盘");
        project.setTags("后端,Java");
        file.setProject(project);
        ProjectExportFile.PhaseItem phase = new ProjectExportFile.PhaseItem();
        phase.setName("开发实现");
        file.setPhases(List.of(phase));
        ProjectExportFile.TaskItem task = new ProjectExportFile.TaskItem();
        task.setTitle("导出项目");
        file.setTasks(List.of(task));
        ProjectExportFile.AttachmentItem attachment = new ProjectExportFile.AttachmentItem();
        attachment.setOriginalFileName("设计图.png");
        attachment.setContentType("image/png");
        attachment.setSizeBytes(3L);
        file.setAttachments(List.of(attachment));
        return file;
    }
}
