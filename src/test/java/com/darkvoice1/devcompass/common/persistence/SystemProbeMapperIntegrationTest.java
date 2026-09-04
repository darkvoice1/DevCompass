package com.darkvoice1.devcompass.common.persistence;

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
import com.darkvoice1.devcompass.common.persistence.entity.SystemProbe;
import com.darkvoice1.devcompass.common.persistence.repository.SystemProbeMapper;

/**
 * 验证 Flyway 和 MyBatis-Plus 的 PostgreSQL 集成闭环。
 */
@Testcontainers
@EnabledIfDockerAvailable
@SpringBootTest(classes = Application.class)
class SystemProbeMapperIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private SystemProbeMapper systemProbeMapper;

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
     * 验证迁移建表后可以完成实体新增和按主键查询。
     */
    @Test
    void shouldInsertAndFindSystemProbe() {
        SystemProbe probe = new SystemProbe();
        probe.setName("persistence-check");

        systemProbeMapper.insert(probe);

        SystemProbe stored = systemProbeMapper.selectById(probe.getId());
        assertThat(stored).isNotNull();
        assertThat(stored.getName()).isEqualTo("persistence-check");
        assertThat(stored.getCreatedAt()).isNotNull();
    }
}
