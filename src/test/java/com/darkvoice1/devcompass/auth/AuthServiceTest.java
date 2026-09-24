package com.darkvoice1.devcompass.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import com.darkvoice1.devcompass.auth.dto.LoginRequest;
import com.darkvoice1.devcompass.auth.dto.TokenResponse;
import com.darkvoice1.devcompass.auth.entity.UserAccount;
import com.darkvoice1.devcompass.auth.service.AuthService;
import com.darkvoice1.devcompass.auth.service.JwtService;
import com.darkvoice1.devcompass.auth.service.UserAccountService;
import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;

/**
 * 验证账号密码登录和 Access Token 签发规则。
 */
class AuthServiceTest {

    private AuthenticationManager authenticationManager;
    private UserAccountService userAccountService;
    private JwtService jwtService;
    private AuthService authService;

    /**
     * 使用模拟依赖初始化登录服务。
     */
    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        userAccountService = mock(UserAccountService.class);
        jwtService = mock(JwtService.class);
        authService = new AuthService(authenticationManager, userAccountService, jwtService);
    }

    /**
     * 验证凭证正确时为启用用户签发 Access Token。
     */
    @Test
    void shouldIssueAccessTokenForValidCredentials() {
        LoginRequest request = loginRequest();
        UserAccount user = user(true);
        TokenResponse token = new TokenResponse("access-token", "Bearer",
                Instant.parse("2026-09-24T10:15:00Z"));
        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any(Authentication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userAccountService.findByUsername("admin")).thenReturn(user);
        when(jwtService.issueAccessToken(user)).thenReturn(token);

        assertThat(authService.login(request)).isSameAs(token);
        verify(jwtService).issueAccessToken(user);
    }

    /**
     * 验证错误凭证统一返回 INVALID_CREDENTIALS，不泄露用户是否存在。
     */
    @Test
    void shouldRejectInvalidCredentials() {
        LoginRequest request = loginRequest();
        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误")
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    /**
     * 验证认证后账号不可用时仍不会签发 Token。
     */
    @Test
    void shouldRejectDisabledUserBeforeIssuingToken() {
        LoginRequest request = loginRequest();
        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any(Authentication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userAccountService.findByUsername("admin")).thenReturn(user(false));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("安全密码123");
        return request;
    }

    private UserAccount user(boolean enabled) {
        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setUsername("admin");
        user.setEnabled(enabled);
        return user;
    }
}
