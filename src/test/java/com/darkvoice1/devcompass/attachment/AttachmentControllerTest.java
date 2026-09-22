package com.darkvoice1.devcompass.attachment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.darkvoice1.devcompass.attachment.controller.AttachmentController;
import com.darkvoice1.devcompass.attachment.dto.AttachmentResponse;
import com.darkvoice1.devcompass.attachment.service.AttachmentService;
import com.darkvoice1.devcompass.common.exception.GlobalExceptionHandler;

/**
 * 验证项目附件上传和列表接口。
 */
class AttachmentControllerTest {

    private AttachmentService attachmentService;

    private MockMvc mockMvc;

    /**
     * 初始化带统一异常处理的 MockMvc。
     */
    @BeforeEach
    void setUp() {
        attachmentService = mock(AttachmentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AttachmentController(attachmentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 验证上传接口返回附件信息，并且不返回存储编号。
     */
    @Test
    void shouldUploadAttachment() throws Exception {
        when(attachmentService.uploadAttachment(org.mockito.ArgumentMatchers.eq(8L),
                org.mockito.ArgumentMatchers.any())).thenReturn(response());

        MockMultipartFile file = new MockMultipartFile(
                "file", "设计图.png", "image/png", new byte[] {1, 2, 3});
        mockMvc.perform(multipart("/api/v1/projects/8/attachments").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.id").value(15))
                .andExpect(jsonPath("$.data.originalFileName").value("设计图.png"))
                .andExpect(jsonPath("$.data.contentType").value("image/png"))
                .andExpect(jsonPath("$.data.sizeBytes").value(3))
                .andExpect(jsonPath("$.data.storageKey").doesNotExist());
    }

    /**
     * 验证没有提交文件时返回统一的参数错误。
     */
    @Test
    void shouldRejectMissingFile() throws Exception {
        mockMvc.perform(multipart("/api/v1/projects/8/attachments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("文件不能为空"));
        verifyNoInteractions(attachmentService);
    }

    /**
     * 验证可以查询项目附件列表。
     */
    @Test
    void shouldListAttachments() throws Exception {
        when(attachmentService.listAttachments(8L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/projects/8/attachments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data[0].originalFileName").value("设计图.png"))
                .andExpect(jsonPath("$.data[0].storageKey").doesNotExist());
    }

    private AttachmentResponse response() {
        AttachmentResponse response = new AttachmentResponse();
        response.setId(15L);
        response.setOriginalFileName("设计图.png");
        response.setContentType("image/png");
        response.setSizeBytes(3L);
        response.setCreatedAt(Instant.parse("2026-09-22T02:00:00Z"));
        return response;
    }
}
