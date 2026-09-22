package com.darkvoice1.devcompass.attachment.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.darkvoice1.devcompass.attachment.dto.AttachmentResponse;
import com.darkvoice1.devcompass.attachment.service.AttachmentService;
import com.darkvoice1.devcompass.common.web.ApiResponse;

/**
 * 提供项目附件的上传和列表接口。
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    /**
     * 创建附件控制器。
     *
     * @param attachmentService 附件业务服务
     */
    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    /**
     * 给项目上传一个附件。表单字段名是 file。
     *
     * @param projectId 项目主键
     * @param file 上传文件
     * @return 附件信息
     */
    @PostMapping
    public ApiResponse<AttachmentResponse> uploadAttachment(
            @PathVariable Long projectId, @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(attachmentService.uploadAttachment(projectId, file));
    }

    /**
     * 查询项目下未删除的附件，新上传的排在前面。
     *
     * @param projectId 项目主键
     * @return 附件列表
     */
    @GetMapping
    public ApiResponse<List<AttachmentResponse>> listAttachments(@PathVariable Long projectId) {
        return ApiResponse.success(attachmentService.listAttachments(projectId));
    }
}
