package com.darkvoice1.devcompass.auth.dto;

import java.time.Instant;

/**
 * Access Token 和 Refresh Token 签发结果。
 */
public class TokenResponse {

    private String accessToken;

    private String tokenType;

    private Instant expiresAt;

    private String refreshToken;

    private Instant refreshExpiresAt;

    public TokenResponse(String accessToken, String tokenType, Instant expiresAt) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresAt = expiresAt;
    }

    public TokenResponse(String accessToken, String tokenType, Instant expiresAt,
            String refreshToken, Instant refreshExpiresAt) {
        this(accessToken, tokenType, expiresAt);
        this.refreshToken = refreshToken;
        this.refreshExpiresAt = refreshExpiresAt;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Instant getRefreshExpiresAt() {
        return refreshExpiresAt;
    }

    public void setRefreshExpiresAt(Instant refreshExpiresAt) {
        this.refreshExpiresAt = refreshExpiresAt;
    }
}
