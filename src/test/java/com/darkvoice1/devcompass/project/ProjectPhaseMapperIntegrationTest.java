package com.darkvoice1.devcompass.project;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.darkvoice1.devcompass.project.entity.ProjectPhase;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.project.repository.ProjectPhaseMapper;

/**
 * 验证项目阶段实体、Flyway 迁移和 MyBatis-Plus 的集成。
 */
@Testcontainers
@EnabledIfDockerAvailable
@SpringBootTest(classes = Application.class)
class ProjectPhaseMapperIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectPhaseMapper projectPhaseMapper;

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
     * 验证项目阶段可新增并按主键查询。
     */
    @Test
    void shouldInsertAndFindProjectPhase() {
        Project project = new Project();
        project.setName("研发罗盘");
        projectMapper.insert(project);

        ProjectPhase phase = new ProjectPhase();
        phase.setProjectId(project.getId());
        phase.setName("需求分析");
        phase.setDescription("梳理项目的核心需求");
        phase.setSortOrder(1);
        projectPhaseMapper.insert(phase);

        ProjectPhase stored = projectPhaseMapper.selectById(phase.getId());
        assertThat(stored).isNotNull();
        assertThat(stored.getProjectId()).isEqualTo(project.getId());
        assertThat(stored.getName()).isEqualTo("需求分析");
        assertThat(stored.getDescription()).isEqualTo("梳理项目的核心需求");
        assertThat(stored.getSortOrder()).isEqualTo(1);
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.getUpdatedAt()).isNotNull();
    }
}
