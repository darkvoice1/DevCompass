package com.darkvoice1.devcompass.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.darkvoice1.devcompass.Application;
import com.darkvoice1.devcompass.attachment.entity.Attachment;
import com.darkvoice1.devcompass.attachment.repository.AttachmentMapper;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;

/**
 * 验证项目附件表和软删除映射。
 */
@Testcontainers
@EnabledIfDockerAvailable
@SpringBootTest(classes = Application.class)
class AttachmentMapperIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private AttachmentMapper attachmentMapper;

    /**
     * 将测试容器连接信息注入 Spring 数据源配置。
     */
    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /**
     * 验证附件可以记到项目下，软删除后普通查询看不到。
     */
    @Test
    void shouldPersistAttachmentAndHideSoftDeletedRow() {
        Project project = project("附件项目");
        Attachment first = attachment(project.getId(), "设计图.png", "image/png", 3,
                "11111111-1111-1111-1111-111111111111");
        Attachment second = attachment(project.getId(), "说明.pdf", "application/pdf", 4,
                "22222222-2222-2222-2222-222222222222");
        attachmentMapper.insert(first);
        attachmentMapper.insert(second);

        Attachment stored = attachmentMapper.selectById(first.getId());
        assertThat(stored.getProjectId()).isEqualTo(project.getId());
        assertThat(stored.getOriginalFileName()).isEqualTo("设计图.png");
        assertThat(stored.getContentType()).isEqualTo("image/png");
        assertThat(stored.getSizeBytes()).isEqualTo(3L);
        assertThat(stored.getStorageKey()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.getDeletedAt()).isNull();

        QueryWrapper<Attachment> query = new QueryWrapper<>();
        query.eq("project_id", project.getId()).orderByDesc("created_at").orderByDesc("id");
        List<Attachment> visible = attachmentMapper.selectList(query);
        assertThat(visible).extracting(item -> item.getId()).containsExactly(second.getId(), first.getId());

        attachmentMapper.deleteById(first.getId());
        assertThat(attachmentMapper.selectById(first.getId())).isNull();
        assertThat(attachmentMapper.selectList(query)).extracting(item -> item.getId())
                .containsExactly(second.getId());
    }

    /**
     * 验证不存在的项目不能写入附件，存储编号也不能重复。
     */
    @Test
    void shouldRejectMissingProjectAndDuplicateStorageKey() {
        Attachment missingProject = attachment(999999L, "设计图.png", "image/png", 3,
                "33333333-3333-3333-3333-333333333333");
        assertThatThrownBy(() -> attachmentMapper.insert(missingProject))
                .isInstanceOf(DataIntegrityViolationException.class);

        Project project = project("重复编号项目");
        attachmentMapper.insert(attachment(project.getId(), "设计图.png", "image/png", 3,
                "44444444-4444-4444-4444-444444444444"));
        assertThatThrownBy(() -> attachmentMapper.insert(attachment(project.getId(), "副本.png", "image/png", 3,
                "44444444-4444-4444-4444-444444444444")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Project project(String name) {
        Project project = new Project();
        project.setName(name);
        projectMapper.insert(project);
        return project;
    }

    private Attachment attachment(Long projectId, String fileName, String contentType, long sizeBytes,
            String storageKey) {
        Attachment attachment = new Attachment();
        attachment.setProjectId(projectId);
        attachment.setOriginalFileName(fileName);
        attachment.setContentType(contentType);
        attachment.setSizeBytes(sizeBytes);
        attachment.setStorageKey(storageKey);
        return attachment;
    }
}
