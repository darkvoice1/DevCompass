package com.darkvoice1.devcompass.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
import org.testcontainers.utility.DockerImageName;

import com.darkvoice1.devcompass.Application;
import com.darkvoice1.devcompass.auth.entity.RefreshToken;
import com.darkvoice1.devcompass.auth.entity.UserAccount;
import com.darkvoice1.devcompass.auth.repository.RefreshTokenMapper;
import com.darkvoice1.devcompass.auth.repository.UserAccountMapper;

/**
 * 验证 Refresh Token 表、外键和轮换字段映射。
 */
@Testcontainers
@EnabledIfDockerAvailable
@SpringBootTest(classes = Application.class)
class RefreshTokenMapperIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine"));

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    /**
     * 将测试数据库和 JWT 配置注入应用。
     */
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("devcompass.auth.jwt-secret",
                () -> "devcompass-test-jwt-secret-change-me-2026");
    }

    /**
     * 验证令牌可以保存、锁定查询并记录轮换关系。
     */
    @Test
    void shouldPersistLockAndRotateRefreshToken() {
        UserAccount user = user("refresh-user");
        RefreshToken oldToken = token(user.getId(), "a".repeat(64), Instant.now().plusSeconds(3600));
        RefreshToken newToken = token(user.getId(), "b".repeat(64), Instant.now().plusSeconds(7200));
        refreshTokenMapper.insert(oldToken);
        refreshTokenMapper.insert(newToken);

        RefreshToken stored = refreshTokenMapper.selectByTokenHashForUpdate(oldToken.getTokenHash());
        assertThat(stored.getUserId()).isEqualTo(user.getId());
        assertThat(stored.getRevokedAt()).isNull();
        assertThat(stored.getCreatedAt()).isNotNull();

        // PostgreSQL TIMESTAMPTZ 保存到微秒，测试值使用相同精度避免纳秒舍入差异。
        Instant revokedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        assertThat(refreshTokenMapper.rotate(oldToken.getId(), revokedAt, newToken.getId())).isEqualTo(1);
        RefreshToken rotated = refreshTokenMapper.selectById(oldToken.getId());
        assertThat(rotated.getRevokedAt()).isEqualTo(revokedAt);
        assertThat(rotated.getReplacedByTokenId()).isEqualTo(newToken.getId());
        assertThat(refreshTokenMapper.rotate(oldToken.getId(), revokedAt, newToken.getId())).isZero();
    }

    /**
     * 验证摘要不能重复，用户外键必须存在。
     */
    @Test
    void shouldRejectDuplicateHashAndMissingUser() {
        UserAccount user = user("unique-refresh-user");
        String tokenHash = "c".repeat(64);
        refreshTokenMapper.insert(token(user.getId(), tokenHash, Instant.now().plusSeconds(3600)));

        assertThatThrownBy(() -> refreshTokenMapper.insert(
                token(user.getId(), tokenHash, Instant.now().plusSeconds(7200))))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> refreshTokenMapper.insert(
                token(999999L, "d".repeat(64), Instant.now().plusSeconds(3600))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private UserAccount user(String username) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash("{bcrypt}test-password-hash");
        user.setEnabled(true);
        userAccountMapper.insert(user);
        return user;
    }

    private RefreshToken token(Long userId, String tokenHash, Instant expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(expiresAt);
        return token;
    }
}
