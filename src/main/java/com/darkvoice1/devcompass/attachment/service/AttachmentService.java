package com.darkvoice1.devcompass.attachment.service;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.darkvoice1.devcompass.attachment.dto.AttachmentResponse;
import com.darkvoice1.devcompass.attachment.entity.Attachment;
import com.darkvoice1.devcompass.attachment.repository.AttachmentMapper;
import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.storage.dto.StoredFile;
import com.darkvoice1.devcompass.storage.service.StorageService;

/**
 * 处理项目附件的上传和列表。
 */
@Service
public class AttachmentService {

    private final ProjectMapper projectMapper;

    private final AttachmentMapper attachmentMapper;

    private final StorageService storageService;

    /**
     * 创建附件服务。
     *
     * @param projectMapper 项目数据访问对象
     * @param attachmentMapper 附件数据访问对象
     * @param storageService 文件存储服务
     */
    public AttachmentService(ProjectMapper projectMapper, AttachmentMapper attachmentMapper,
            StorageService storageService) {
        this.projectMapper = projectMapper;
        this.attachmentMapper = attachmentMapper;
        this.storageService = storageService;
    }

    /**
     * 给项目上传一个附件。先保存文件，再写入数据库。
     *
     * @param projectId 项目主键
     * @param file 上传文件
     * @return 附件信息，不包含磁盘路径和存储编号
     */
    @Transactional
    public AttachmentResponse uploadAttachment(Long projectId, MultipartFile file) {
        findProjectOrThrow(projectId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "文件不能为空");
        }
        StoredFile stored = storageService.save(file.getOriginalFilename(), readContent(file));
        Attachment attachment = new Attachment();
        attachment.setProjectId(projectId);
        attachment.setOriginalFileName(stored.getOriginalFileName());
        attachment.setContentType(stored.getContentType());
        attachment.setSizeBytes(stored.getSizeBytes());
        attachment.setStorageKey(stored.getStorageKey());
        try {
            attachmentMapper.insert(attachment);
        } catch (RuntimeException exception) {
            // 数据库没有记下这条附件时，删掉刚写入磁盘的文件。
            deleteQuietly(stored.getStorageKey(), exception);
            throw exception;
        }
        return toResponse(attachmentMapper.selectById(attachment.getId()));
    }

    /**
     * 查询项目下未删除的附件，新上传的排在前面。
     *
     * @param projectId 项目主键
     * @return 附件列表
     */
    public List<AttachmentResponse> listAttachments(Long projectId) {
        findProjectOrThrow(projectId);
        QueryWrapper<Attachment> query = new QueryWrapper<>();
        query.eq("project_id", projectId)
                .orderByDesc("created_at")
                .orderByDesc("id");
        return attachmentMapper.selectList(query).stream().map(this::toResponse).toList();
    }

    /**
     * 查询项目。已删除项目查不到，已归档项目仍然可以上传和查看附件。
     *
     * @param projectId 项目主键
     */
    private void findProjectOrThrow(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "项目不存在");
        }
    }

    /**
     * 读取上传文件的字节内容。
     *
     * @param file 上传文件
     * @return 文件内容
     */
    private byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            BusinessException businessException = new BusinessException(ErrorCode.INTERNAL_ERROR, "读取上传文件失败");
            businessException.initCause(exception);
            throw businessException;
        }
    }

    /**
     * 清理已经写入磁盘、但数据库没有保存成功的文件。
     *
     * @param storageKey 存储编号
     * @param exception 写库失败异常
     */
    private void deleteQuietly(String storageKey, RuntimeException exception) {
        try {
            storageService.delete(storageKey);
        } catch (RuntimeException deleteException) {
            exception.addSuppressed(deleteException);
        }
    }

    /**
     * 转换成接口响应，不带出存储编号。
     *
     * @param attachment 附件实体
     * @return 附件响应
     */
    private AttachmentResponse toResponse(Attachment attachment) {
        AttachmentResponse response = new AttachmentResponse();
        response.setId(attachment.getId());
        response.setOriginalFileName(attachment.getOriginalFileName());
        response.setContentType(attachment.getContentType());
        response.setSizeBytes(attachment.getSizeBytes());
        response.setCreatedAt(attachment.getCreatedAt());
        return response;
    }
}
