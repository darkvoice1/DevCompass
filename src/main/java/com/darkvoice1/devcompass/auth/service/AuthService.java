package com.darkvoice1.devcompass.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.darkvoice1.devcompass.auth.dto.LoginRequest;
import com.darkvoice1.devcompass.auth.dto.TokenResponse;
import com.darkvoice1.devcompass.auth.entity.UserAccount;
import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;

/**
 * 处理账号密码登录并签发 Access Token。
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserAccountService userAccountService;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager,
            UserAccountService userAccountService, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userAccountService = userAccountService;
        this.jwtService = jwtService;
    }

    /**
     * 校验账号密码并签发 Access Token。
     *
     * @param request 登录请求
     * @return Token 签发结果
     */
    public TokenResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException | DisabledException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_CREDENTIALS, ErrorCode.INVALID_CREDENTIALS.getMessage());
        }

        UserAccount user = userAccountService.findByUsername(request.getUsername());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(
                    ErrorCode.INVALID_CREDENTIALS, ErrorCode.INVALID_CREDENTIALS.getMessage());
        }
        return jwtService.issueAccessToken(user);
    }
}
