package com.darkvoice1.devcompass.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import com.darkvoice1.devcompass.auth.dto.TokenResponse;
import com.darkvoice1.devcompass.auth.entity.UserAccount;
import com.darkvoice1.devcompass.common.time.DevCompassProperties;

/**
 * 签发短期 Access Token。
 */
@Service
public class JwtService {

    private static final String TOKEN_ISSUER = "https://devcompass.local";

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final Duration accessTokenExpiration;

    public JwtService(JwtEncoder jwtEncoder, Clock clock, DevCompassProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        Duration configuredExpiration = properties.getAuth() == null
                ? null : properties.getAuth().getAccessTokenExpiration();
        if (configuredExpiration == null || configuredExpiration.isZero()
                || configuredExpiration.isNegative()) {
            throw new IllegalStateException("Access Token 有效期必须大于0");
        }
        this.accessTokenExpiration = configuredExpiration;
    }

    /**
     * 为已认证用户签发 Access Token。
     *
     * @param user 用户账号
     * @return Access Token 及过期时间
     */
    public TokenResponse issueAccessToken(UserAccount user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(accessTokenExpiration);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(TOKEN_ISSUER)
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("username", user.getUsername())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", expiresAt);
    }
}
