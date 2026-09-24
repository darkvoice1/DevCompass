package com.darkvoice1.devcompass.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.darkvoice1.devcompass.auth.dto.IssuedRefreshToken;
import com.darkvoice1.devcompass.auth.dto.TokenResponse;
import com.darkvoice1.devcompass.auth.entity.RefreshToken;
import com.darkvoice1.devcompass.auth.entity.UserAccount;
import com.darkvoice1.devcompass.auth.repository.RefreshTokenMapper;
import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.common.time.DevCompassProperties;

/**
 * 签发、校验并轮换 Refresh Token。
 */
@Service
public class RefreshTokenService {

    private static final int TOKEN_RANDOM_BYTES = 32;

    private final RefreshTokenMapper refreshTokenMapper;
    private final UserAccountService userAccountService;
    private final JwtService jwtService;
    private final Clock clock;
    private final Duration refreshTokenExpiration;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenMapper refreshTokenMapper,
            UserAccountService userAccountService, JwtService jwtService,
            Clock clock, DevCompassProperties properties) {
        this.refreshTokenMapper = refreshTokenMapper;
        this.userAccountService = userAccountService;
        this.jwtService = jwtService;
        this.clock = clock;
        Duration configuredExpiration = properties.getAuth() == null
                ? null : properties.getAuth().getRefreshTokenExpiration();
        if (configuredExpiration == null || configuredExpiration.isZero()
                || configuredExpiration.isNegative()) {
            throw new IllegalStateException("Refresh Token 有效期必须大于0");
        }
        this.refreshTokenExpiration = configuredExpiration;
    }

    /**
     * 为用户签发新的 Refresh Token，数据库只保存摘要。
     *
     * @param userId 用户主键
     * @return 原始令牌、过期时间和数据库主键
     */
    public IssuedRefreshToken issue(Long userId) {
        String rawToken = generateToken();
        Instant expiresAt = clock.instant().plus(refreshTokenExpiration);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setExpiresAt(expiresAt);
        refreshTokenMapper.insert(refreshToken);
        return new IssuedRefreshToken(refreshToken.getId(), rawToken, expiresAt);
    }

    /**
     * 使用一次旧 Refresh Token，轮换出一整套新令牌。
     *
     * @param rawToken 原始 Refresh Token
     * @return 新的 Access Token 和 Refresh Token
     */
    @Transactional
    public TokenResponse rotate(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw invalidRefreshToken();
        }

        RefreshToken storedToken = refreshTokenMapper.selectByTokenHashForUpdate(hash(rawToken));
        if (storedToken == null || storedToken.getRevokedAt() != null) {
            throw invalidRefreshToken();
        }

        Instant now = clock.instant();
        if (!storedToken.getExpiresAt().isAfter(now)) {
            throw new BusinessException(
                    ErrorCode.REFRESH_TOKEN_EXPIRED, ErrorCode.REFRESH_TOKEN_EXPIRED.getMessage());
        }

        UserAccount user = userAccountService.findById(storedToken.getUserId());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw invalidRefreshToken();
        }

        IssuedRefreshToken replacement = issue(user.getId());
        int updated = refreshTokenMapper.rotate(storedToken.getId(), now, replacement.getId());
        if (updated != 1) {
            throw invalidRefreshToken();
        }

        TokenResponse accessToken = jwtService.issueAccessToken(user);
        return withRefreshToken(accessToken, replacement);
    }

    /**
     * 把 Refresh Token 添加到 Access Token 响应中。
     *
     * @param accessToken Access Token 结果
     * @param refreshToken Refresh Token 结果
     * @return 完整令牌响应
     */
    public TokenResponse withRefreshToken(
            TokenResponse accessToken, IssuedRefreshToken refreshToken) {
        return new TokenResponse(accessToken.getAccessToken(), accessToken.getTokenType(),
                accessToken.getExpiresAt(), refreshToken.getToken(), refreshToken.getExpiresAt());
    }

    private String generateToken() {
        byte[] randomBytes = new byte[TOKEN_RANDOM_BYTES];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 Java 环境不支持 SHA-256", exception);
        }
    }

    private BusinessException invalidRefreshToken() {
        return new BusinessException(
                ErrorCode.INVALID_REFRESH_TOKEN, ErrorCode.INVALID_REFRESH_TOKEN.getMessage());
    }
}
