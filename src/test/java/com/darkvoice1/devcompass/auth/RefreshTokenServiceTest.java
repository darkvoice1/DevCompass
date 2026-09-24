package com.darkvoice1.devcompass.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.darkvoice1.devcompass.auth.dto.IssuedRefreshToken;
import com.darkvoice1.devcompass.auth.dto.TokenResponse;
import com.darkvoice1.devcompass.auth.entity.RefreshToken;
import com.darkvoice1.devcompass.auth.entity.UserAccount;
import com.darkvoice1.devcompass.auth.repository.RefreshTokenMapper;
import com.darkvoice1.devcompass.auth.service.JwtService;
import com.darkvoice1.devcompass.auth.service.RefreshTokenService;
import com.darkvoice1.devcompass.auth.service.UserAccountService;
import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.common.time.DevCompassProperties;

/**
 * 验证 Refresh Token 签发、摘要存储和轮换规则。
 */
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");

    private RefreshTokenMapper refreshTokenMapper;
    private UserAccountService userAccountService;
    private JwtService jwtService;
    private RefreshTokenService refreshTokenService;

    /**
     * 使用固定时钟和模拟依赖初始化服务。
     */
    @BeforeEach
    void setUp() {
        refreshTokenMapper = mock(RefreshTokenMapper.class);
        userAccountService = mock(UserAccountService.class);
        jwtService = mock(JwtService.class);
        refreshTokenService = new RefreshTokenService(
                refreshTokenMapper, userAccountService, jwtService,
                Clock.fixed(NOW, ZoneOffset.UTC), properties(Duration.ofDays(7)));
    }

    /**
     * 验证数据库只保存 64 位 SHA-256 摘要，不保存原始令牌。
     */
    @Test
    void shouldStoreOnlyRefreshTokenHash() {
        when(refreshTokenMapper.insert(org.mockito.ArgumentMatchers.any(RefreshToken.class)))
                .thenAnswer(invocation -> {
                    RefreshToken token = invocation.getArgument(0);
                    token.setId(11L);
                    return 1;
                });

        IssuedRefreshToken issued = refreshTokenService.issue(7L);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenMapper).insert(captor.capture());
        RefreshToken stored = captor.getValue();
        assertThat(issued.getToken()).isNotBlank().doesNotContain("=");
        assertThat(stored.getTokenHash()).hasSize(64).isEqualTo(hash(issued.getToken()));
        assertThat(stored.getTokenHash()).isNotEqualTo(issued.getToken());
        assertThat(stored.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        assertThat(issued.getId()).isEqualTo(11L);
    }

    /**
     * 验证有效旧令牌会被撤销，并返回一整套新令牌。
     */
    @Test
    void shouldRotateValidRefreshToken() {
        String oldRawToken = "old-refresh-token";
        RefreshToken oldToken = refreshToken(3L, 7L, NOW.plusSeconds(60), null);
        UserAccount user = user();
        TokenResponse accessToken = new TokenResponse(
                "new-access", "Bearer", NOW.plusSeconds(900));
        when(refreshTokenMapper.selectByTokenHashForUpdate(hash(oldRawToken))).thenReturn(oldToken);
        when(userAccountService.findById(7L)).thenReturn(user);
        when(refreshTokenMapper.insert(org.mockito.ArgumentMatchers.any(RefreshToken.class)))
                .thenAnswer(invocation -> {
                    RefreshToken token = invocation.getArgument(0);
                    token.setId(4L);
                    return 1;
                });
        when(refreshTokenMapper.rotate(3L, NOW, 4L)).thenReturn(1);
        when(jwtService.issueAccessToken(user)).thenReturn(accessToken);

        TokenResponse response = refreshTokenService.rotate(oldRawToken);

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isNotBlank().isNotEqualTo(oldRawToken);
        assertThat(response.getRefreshExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        verify(refreshTokenMapper).rotate(3L, NOW, 4L);
    }

    /**
     * 验证不存在、已撤销和并发失效的令牌统一视为无效。
     */
    @Test
    void shouldRejectInvalidOrAlreadyUsedRefreshToken() {
        assertInvalidRefreshToken(() -> refreshTokenService.rotate("missing-token"));

        RefreshToken revoked = refreshToken(3L, 7L, NOW.plusSeconds(60), NOW.minusSeconds(1));
        when(refreshTokenMapper.selectByTokenHashForUpdate(hash("revoked-token"))).thenReturn(revoked);
        assertInvalidRefreshToken(() -> refreshTokenService.rotate("revoked-token"));

        RefreshToken concurrent = refreshToken(5L, 7L, NOW.plusSeconds(60), null);
        when(refreshTokenMapper.selectByTokenHashForUpdate(hash("concurrent-token"))).thenReturn(concurrent);
        when(userAccountService.findById(7L)).thenReturn(user());
        when(refreshTokenMapper.insert(org.mockito.ArgumentMatchers.any(RefreshToken.class)))
                .thenAnswer(invocation -> {
                    RefreshToken token = invocation.getArgument(0);
                    token.setId(6L);
                    return 1;
                });
        when(refreshTokenMapper.rotate(5L, NOW, 6L)).thenReturn(0);
        assertInvalidRefreshToken(() -> refreshTokenService.rotate("concurrent-token"));
    }

    /**
     * 验证过期令牌返回独立错误码。
     */
    @Test
    void shouldRejectExpiredRefreshToken() {
        RefreshToken expired = refreshToken(3L, 7L, NOW, null);
        when(refreshTokenMapper.selectByTokenHashForUpdate(hash("expired-token"))).thenReturn(expired);

        assertThatThrownBy(() -> refreshTokenService.rotate("expired-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Refresh Token 已过期")
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.REFRESH_TOKEN_EXPIRED));
    }

    /**
     * 验证有效期配置必须大于 0。
     */
    @Test
    void shouldRejectNonPositiveRefreshTokenExpiration() {
        assertThatThrownBy(() -> new RefreshTokenService(
                refreshTokenMapper, userAccountService, jwtService,
                Clock.systemUTC(), properties(Duration.ZERO)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Refresh Token 有效期必须大于0");
    }

    private RefreshToken refreshToken(Long id, Long userId, Instant expiresAt, Instant revokedAt) {
        RefreshToken token = new RefreshToken();
        token.setId(id);
        token.setUserId(userId);
        token.setExpiresAt(expiresAt);
        token.setRevokedAt(revokedAt);
        return token;
    }

    private UserAccount user() {
        UserAccount user = new UserAccount();
        user.setId(7L);
        user.setUsername("admin");
        user.setEnabled(true);
        return user;
    }

    private DevCompassProperties properties(Duration expiration) {
        DevCompassProperties properties = new DevCompassProperties();
        properties.getAuth().setRefreshTokenExpiration(expiration);
        return properties;
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void assertInvalidRefreshToken(ThrowableAction action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .hasMessage("Refresh Token 无效")
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    @FunctionalInterface
    private interface ThrowableAction {
        void run();
    }
}
