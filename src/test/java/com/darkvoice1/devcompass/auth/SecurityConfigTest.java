package com.darkvoice1.devcompass.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import com.darkvoice1.devcompass.auth.service.JwtService;
import com.darkvoice1.devcompass.common.time.DevCompassProperties;
import com.darkvoice1.devcompass.security.SecurityConfig;

/**
 * 验证 JWT 密钥和有效期配置边界。
 */
class SecurityConfigTest {

    /**
     * 验证 JWT 密钥不能为空且不能短于 32 字节。
     */
    @Test
    void shouldRejectMissingOrShortJwtSecret() {
        SecurityConfig securityConfig = new SecurityConfig();
        DevCompassProperties missing = new DevCompassProperties();

        assertThatThrownBy(() -> securityConfig.jwtEncoder(missing))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT 签名密钥不能为空");

        DevCompassProperties shortSecret = new DevCompassProperties();
        shortSecret.getAuth().setJwtSecret("too-short");
        assertThatThrownBy(() -> securityConfig.jwtDecoder(shortSecret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT 签名密钥的 UTF-8 编码长度不能少于32字节");
    }

    /**
     * 验证 Access Token 有效期必须为正数。
     */
    @Test
    void shouldRejectNonPositiveAccessTokenExpiration() {
        DevCompassProperties properties = new DevCompassProperties();
        properties.getAuth().setJwtSecret("devcompass-test-jwt-secret-change-me-2026");
        properties.getAuth().setAccessTokenExpiration(Duration.ZERO);
        JwtEncoder jwtEncoder = new SecurityConfig().jwtEncoder(properties);

        assertThatThrownBy(() -> new JwtService(jwtEncoder, Clock.systemUTC(), properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Access Token 有效期必须大于0");
    }
}
