package com.darkvoice1.devcompass.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import com.darkvoice1.devcompass.auth.dto.TokenResponse;
import com.darkvoice1.devcompass.auth.entity.UserAccount;
import com.darkvoice1.devcompass.auth.service.JwtService;
import com.darkvoice1.devcompass.common.time.DevCompassProperties;
import com.darkvoice1.devcompass.security.SecurityConfig;

/**
 * 验证 Access Token 的内容、签名和有效期。
 */
class JwtServiceTest {

    /**
     * 验证签发的 Token 可通过同一密钥验签，并包含必要用户信息。
     */
    @Test
    void shouldIssueVerifiableAccessTokenWithRequiredClaims() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        DevCompassProperties properties = properties();
        SecurityConfig securityConfig = new SecurityConfig();
        JwtEncoder encoder = securityConfig.jwtEncoder(properties);
        JwtDecoder decoder = securityConfig.jwtDecoder(properties);
        JwtService jwtService = new JwtService(
                encoder, Clock.fixed(now, ZoneOffset.UTC), properties);
        UserAccount user = new UserAccount();
        user.setId(7L);
        user.setUsername("admin");

        TokenResponse response = jwtService.issueAccessToken(user);
        Jwt jwt = decoder.decode(response.getAccessToken());

        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(15)));
        assertThat(jwt.getIssuer().toString()).isEqualTo("https://devcompass.local");
        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(15)));
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getClaimAsString("username")).isEqualTo("admin");
    }

    private DevCompassProperties properties() {
        DevCompassProperties properties = new DevCompassProperties();
        properties.getAuth().setJwtSecret("devcompass-test-jwt-secret-change-me-2026");
        properties.getAuth().setAccessTokenExpiration(Duration.ofMinutes(15));
        return properties;
    }
}
