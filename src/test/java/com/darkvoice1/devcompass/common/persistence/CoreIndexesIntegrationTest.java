package com.darkvoice1.devcompass.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.darkvoice1.devcompass.Application;

/**
 * 验证核心查询索引已通过 Flyway 正确创建。
 */
@Testcontainers
@EnabledIfDockerAvailable
@SpringBootTest(classes = Application.class)
class CoreIndexesIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
     * 验证 V8 迁移创建核心查询索引。
     */
    @Test
    void shouldCreateCoreQueryIndexes() {
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes "
                        + "WHERE schemaname = 'public' AND indexname IN "
                        + "('idx_project_active_status', "
                        + "'idx_project_phase_active_project_sort', "
                        + "'idx_task_active_project_status_priority_due', "
                        + "'idx_task_active_project_phase_due') "
                        + "ORDER BY indexname",
                String.class);

        assertThat(indexes).containsExactly(
                "idx_project_active_status",
                "idx_project_phase_active_project_sort",
                "idx_task_active_project_phase_due",
                "idx_task_active_project_status_priority_due");
    }
}
