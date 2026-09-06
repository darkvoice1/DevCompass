package com.darkvoice1.devcompass.project;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.darkvoice1.devcompass.Application;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.entity.ProjectStatus;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;

/**
 * 验证项目实体、Flyway 迁移和 MyBatis-Plus 的集成。
 */
@Testcontainers
@EnabledIfDockerAvailable
@SpringBootTest(classes = Application.class)
class ProjectMapperIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ProjectMapper projectMapper;

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
     * 验证项目字段可以新增并按主键查询。
     */
    @Test
    void shouldInsertAndFindProject() {
        Project project = new Project();
        project.setName("研发罗盘");
        project.setDescription("个人研发管理平台");
        project.setStatus(ProjectStatus.IN_PROGRESS);
        project.setTargetDate(LocalDate.of(2026, 12, 31));
        project.setTechStack("Java,Spring Boot");

        projectMapper.insert(project);

        Project stored = projectMapper.selectById(project.getId());
        assertThat(stored).isNotNull();
        assertThat(stored.getName()).isEqualTo("研发罗盘");
        assertThat(stored.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(stored.getTargetDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(stored.getTechStack()).isEqualTo("Java,Spring Boot");
        assertThat(stored.isArchived()).isFalse();
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.getUpdatedAt()).isNotNull();
    }
}
