package com.darkvoice1.devcompass.auth.controller;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.darkvoice1.devcompass.auth.dto.CurrentUserResponse;
import com.darkvoice1.devcompass.auth.dto.LoginRequest;
import com.darkvoice1.devcompass.auth.dto.TokenResponse;
import com.darkvoice1.devcompass.auth.service.AuthService;
import com.darkvoice1.devcompass.common.web.ApiResponse;

import jakarta.validation.Valid;

/**
 * 提供登录和当前用户查询接口。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 使用用户名和密码登录。
     *
     * @param request 登录请求
     * @return Access Token
     */
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * 返回当前 Access Token 代表的用户。
     *
     * @param jwt 已验证的 JWT
     * @return 当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> currentUser(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(new CurrentUserResponse(
                Long.valueOf(jwt.getSubject()), jwt.getClaimAsString("username")));
    }
}
