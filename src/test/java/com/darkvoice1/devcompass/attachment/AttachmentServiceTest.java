package com.darkvoice1.devcompass.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.darkvoice1.devcompass.attachment.dto.AttachmentContent;
import com.darkvoice1.devcompass.attachment.dto.AttachmentResponse;
import com.darkvoice1.devcompass.attachment.entity.Attachment;
import com.darkvoice1.devcompass.attachment.repository.AttachmentMapper;
import com.darkvoice1.devcompass.attachment.service.AttachmentService;
import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.storage.dto.StoredFile;
import com.darkvoice1.devcompass.storage.service.StorageService;

/**
 * 验证项目附件上传、列表、下载和删除。
 */
class AttachmentServiceTest {

    private ProjectMapper projectMapper;

    private AttachmentMapper attachmentMapper;

    private StorageService storageService;

    private AttachmentService attachmentService;

    /**
     * 初始化附件服务及模拟依赖。
     */
    @BeforeEach
    void setUp() {
        projectMapper = mock(ProjectMapper.class);
        attachmentMapper = mock(AttachmentMapper.class);
        storageService = mock(StorageService.class);
        attachmentService = new AttachmentService(projectMapper, attachmentMapper, storageService);
    }

    /**
     * 验证上传时先保存文件，再记下元数据，响应里没有存储编号。
     */
    @Test
    void shouldUploadAttachmentWithoutExposingStorageKey() throws IOException {
        when(projectMapper.selectById(8L)).thenReturn(project(false));
        when(storageService.save("设计图.png", new byte[] {1, 2, 3})).thenReturn(storedFile());
        doAnswer(invocation -> {
            invocation.<Attachment>getArgument(0).setId(15L);
            return 1;
        }).when(attachmentMapper).insert(any(Attachment.class));
        when(attachmentMapper.selectById(15L)).thenReturn(savedAttachment());

        AttachmentResponse response = attachmentService.uploadAttachment(8L, file("设计图.png", new byte[] {1, 2, 3}));

        assertThat(response.getId()).isEqualTo(15L);
        assertThat(response.getOriginalFileName()).isEqualTo("设计图.png");
        assertThat(response.getContentType()).isEqualTo("image/png");
        assertThat(response.getSizeBytes()).isEqualTo(3L);
        assertThat(response.getCreatedAt()).isEqualTo(Instant.parse("2026-09-22T02:00:00Z"));
        ArgumentCaptor<Attachment> captor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentMapper).insert(captor.capture());
        assertThat(captor.getValue().getProjectId()).isEqualTo(8L);
        assertThat(captor.getValue().getStorageKey()).isEqualTo("11111111-1111-1111-1111-111111111111");
    }

    /**
     * 验证已归档项目仍然可以上传附件。
     */
    @Test
    void shouldUploadAttachmentForArchivedProject() throws IOException {
        when(projectMapper.selectById(8L)).thenReturn(project(true));
        when(storageService.save(any(), any())).thenReturn(storedFile());
        doAnswer(invocation -> {
            invocation.<Attachment>getArgument(0).setId(15L);
            return 1;
        }).when(attachmentMapper).insert(any(Attachment.class));
        when(attachmentMapper.selectById(15L)).thenReturn(savedAttachment());

        AttachmentResponse response = attachmentService.uploadAttachment(8L, file("设计图.png", new byte[] {1}));

        assertThat(response.getId()).isEqualTo(15L);
        verify(storageService).save("设计图.png", new byte[] {1});
    }

    /**
     * 验证项目不存在时不保存文件。
     */
    @Test
    void shouldRejectUploadWhenProjectIsMissing() throws IOException {
        when(projectMapper.selectById(8L)).thenReturn(null);

        assertThatThrownBy(() -> attachmentService.uploadAttachment(8L, file("设计图.png", new byte[] {1})))
                .isInstanceOf(BusinessException.class)
                .hasMessage("项目不存在");
        verifyNoInteractions(storageService);
        verify(attachmentMapper, never()).insert(any(Attachment.class));
    }

    /**
     * 验证空文件在保存前被拒绝。
     */
    @Test
    void shouldRejectEmptyUpload() {
        when(projectMapper.selectById(8L)).thenReturn(project(false));
        MultipartFile emptyFile = mock(MultipartFile.class);
        when(emptyFile.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> attachmentService.uploadAttachment(8L, emptyFile))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件不能为空")
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
        verifyNoInteractions(storageService);
    }

    /**
     * 验证存储校验失败时不写数据库。
     */
    @Test
    void shouldNotInsertWhenStorageRejectsFile() throws IOException {
        when(projectMapper.selectById(8L)).thenReturn(project(false));
        when(storageService.save(any(), any()))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "不支持的文件类型"));

        assertThatThrownBy(() -> attachmentService.uploadAttachment(8L, file("脚本.exe", new byte[] {1})))
                .isInstanceOf(BusinessException.class)
                .hasMessage("不支持的文件类型");
        verify(attachmentMapper, never()).insert(any(Attachment.class));
    }

    /**
     * 验证数据库写入失败时删掉刚保存的文件。
     */
    @Test
    void shouldDeleteStoredFileWhenInsertFails() throws IOException {
        when(projectMapper.selectById(8L)).thenReturn(project(false));
        when(storageService.save(any(), any())).thenReturn(storedFile());
        when(attachmentMapper.insert(any(Attachment.class))).thenThrow(new IllegalStateException("写库失败"));

        assertThatThrownBy(() -> attachmentService.uploadAttachment(8L, file("设计图.png", new byte[] {1})))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("写库失败");
        verify(storageService).delete("11111111-1111-1111-1111-111111111111");
    }

    /**
     * 验证列表按创建时间倒序返回，并且项目不存在时不查询附件。
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldListAttachmentsFromNewest() {
        when(projectMapper.selectById(8L)).thenReturn(project(false));
        Attachment newer = savedAttachment();
        newer.setId(16L);
        newer.setOriginalFileName("说明.pdf");
        when(attachmentMapper.selectList(any())).thenReturn(List.of(newer, savedAttachment()));

        List<AttachmentResponse> responses = attachmentService.listAttachments(8L);

        assertThat(responses).extracting(item -> item.getOriginalFileName())
                .containsExactly("说明.pdf", "设计图.png");
        ArgumentCaptor<QueryWrapper<Attachment>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(attachmentMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("project_id");
        assertThat(captor.getValue().getCustomSqlSegment()).contains("created_at").contains("id");

        when(projectMapper.selectById(9L)).thenReturn(null);
        assertThatThrownBy(() -> attachmentService.listAttachments(9L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("项目不存在");
    }

    /**
     * 验证可以下载属于该项目的附件内容。
     */
    @Test
    void shouldDownloadAttachmentContent() {
        when(projectMapper.selectById(8L)).thenReturn(project(false));
        when(attachmentMapper.selectById(15L)).thenReturn(savedAttachment());
        when(storageService.load("11111111-1111-1111-1111-111111111111")).thenReturn(new byte[] {1, 2, 3});

        AttachmentContent content = attachmentService.downloadAttachment(8L, 15L);

        assertThat(content.getOriginalFileName()).isEqualTo("设计图.png");
        assertThat(content.getContentType()).isEqualTo("image/png");
        assertThat(content.getContent()).containsExactly(1, 2, 3);
    }

    /**
     * 验证其他项目的附件编号不能下载。
     */
    @Test
    void shouldRejectDownloadWhenAttachmentBelongsToAnotherProject() {
        when(projectMapper.selectById(8L)).thenReturn(project(false));
        Attachment anotherProject = savedAttachment();
        anotherProject.setProjectId(9L);
        when(attachmentMapper.selectById(15L)).thenReturn(anotherProject);

        assertThatThrownBy(() -> attachmentService.downloadAttachment(8L, 15L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("附件不存在");
        verifyNoInteractions(storageService);
    }

    /**
     * 验证附件不存在或项目不存在时不能下载。
     */
    @Test
    void shouldRejectDownloadWhenAttachmentOrProjectIsMissing() {
        when(projectMapper.selectById(8L)).thenReturn(project(false));
        when(attachmentMapper.selectById(15L)).thenReturn(null);
        assertThatThrownBy(() -> attachmentService.downloadAttachment(8L, 15L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("附件不存在");

        when(projectMapper.selectById(9L)).thenReturn(null);
        assertThatThrownBy(() -> attachmentService.downloadAttachment(9L, 15L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("项目不存在");
    }

    /**
     * 验证删除时同时软删除记录和磁盘文件。
     */
    @Test
    void shouldDeleteAttachmentAndStoredFile() {
        when(projectMapper.selectById(8L)).thenReturn(project(true));
        when(attachmentMapper.selectById(15L)).thenReturn(savedAttachment());
        when(attachmentMapper.deleteById(15L)).thenReturn(1);

        attachmentService.deleteAttachment(8L, 15L);

        verify(storageService).delete("11111111-1111-1111-1111-111111111111");
        verify(attachmentMapper).deleteById(15L);
    }

    /**
     * 验证删文件失败时不把记录标成已删除。
     */
    @Test
    void shouldKeepRecordWhenStoredFileCannotBeDeleted() {
        when(projectMapper.selectById(8L)).thenReturn(project(false));
        when(attachmentMapper.selectById(15L)).thenReturn(savedAttachment());
        doThrow(new BusinessException(ErrorCode.INTERNAL_ERROR, "删除文件失败"))
                .when(storageService).delete("11111111-1111-1111-1111-111111111111");

        assertThatThrownBy(() -> attachmentService.deleteAttachment(8L, 15L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("删除文件失败");
        verify(attachmentMapper, never()).deleteById(any(Long.class));
    }

    private Project project(boolean archived) {
        Project project = new Project();
        project.setId(8L);
        project.setName("研发罗盘");
        project.setArchived(archived);
        return project;
    }

    private StoredFile storedFile() {
        return new StoredFile("11111111-1111-1111-1111-111111111111", "设计图.png", "image/png", 3);
    }

    private Attachment savedAttachment() {
        Attachment attachment = new Attachment();
        attachment.setId(15L);
        attachment.setProjectId(8L);
        attachment.setOriginalFileName("设计图.png");
        attachment.setContentType("image/png");
        attachment.setSizeBytes(3L);
        attachment.setStorageKey("11111111-1111-1111-1111-111111111111");
        attachment.setCreatedAt(Instant.parse("2026-09-22T02:00:00Z"));
        return attachment;
    }

    private MultipartFile file(String fileName, byte[] content) throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn(fileName);
        when(file.getBytes()).thenReturn(content);
        return file;
    }
}
