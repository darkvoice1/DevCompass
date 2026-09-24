package com.darkvoice1.devcompass.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

import com.darkvoice1.devcompass.Application;
import com.darkvoice1.devcompass.auth.entity.UserAccount;
import com.darkvoice1.devcompass.auth.repository.UserAccountMapper;

/**
 * 验证用户表、Flyway 迁移和 MyBatis-Plus 映射。
 */
@Testcontainers
@EnabledIfDockerAvailable
@SpringBootTest(classes = Application.class)
class UserAccountMapperIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private UserAccountMapper userAccountMapper;

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
     * 验证用户可以保存，并能用不同大小写的用户名查询。
     */
    @Test
    void shouldPersistAndFindUserIgnoringUsernameCase() {
        UserAccount user = user("reader", "{bcrypt}$2a$10$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu");
        userAccountMapper.insert(user);

        UserAccount stored = userAccountMapper.selectByUsername("READER");
        assertThat(stored).isNotNull();
        assertThat(stored.getId()).isEqualTo(user.getId());
        assertThat(stored.getUsername()).isEqualTo("reader");
        assertThat(stored.getPasswordHash()).startsWith("{bcrypt}");
        assertThat(stored.getEnabled()).isTrue();
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.getUpdatedAt()).isNotNull();
        assertThat(userAccountMapper.countAll()).isPositive();
    }

    /**
     * 验证用户名唯一性不区分大小写。
     */
    @Test
    void shouldRejectDuplicateUsernameIgnoringCase() {
        userAccountMapper.insert(user("admin", "{bcrypt}first"));

        assertThatThrownBy(() -> userAccountMapper.insert(user("ADMIN", "{bcrypt}second")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private UserAccount user(String username, String passwordHash) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setEnabled(true);
        return user;
    }
}
