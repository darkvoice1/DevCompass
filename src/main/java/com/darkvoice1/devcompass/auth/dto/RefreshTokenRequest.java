package com.darkvoice1.devcompass.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新令牌请求。
 */
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh Token 不能为空")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
