package com.darkvoice1.devcompass.auth.dto;

import java.time.Instant;

/**
 * 新签发的 Refresh Token 及其持久化编号。
 */
public class IssuedRefreshToken {

    private final Long id;

    private final String token;

    private final Instant expiresAt;

    public IssuedRefreshToken(Long id, String token, Instant expiresAt) {
        this.id = id;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
