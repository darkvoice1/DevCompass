package com.darkvoice1.devcompass.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.darkvoice1.devcompass.auth.service.AuthUserDetailsService;
import com.darkvoice1.devcompass.common.time.DevCompassProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * 配置账号密码认证和 JWT 请求认证。
 */
@Configuration
public class SecurityConfig {

    private static final int MIN_SECRET_BYTES = 32;

    /**
     * 配置无状态 API 安全规则。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, RestAuthenticationEntryPoint authenticationEntryPoint) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .jwt(Customizer.withDefaults()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint));
        return http.build();
    }

    /**
     * 创建使用项目用户表和密码编码器的认证管理器。
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * 创建 JWT 签发器。
     */
    @Bean
    public JwtEncoder jwtEncoder(DevCompassProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecret(properties)));
    }

    /**
     * 创建只接受 HS256 的 JWT 验证器。
     */
    @Bean
    public JwtDecoder jwtDecoder(DevCompassProperties properties) {
        return NimbusJwtDecoder.withSecretKey(jwtSecret(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private SecretKey jwtSecret(DevCompassProperties properties) {
        String secret = properties.getAuth() == null ? null : properties.getAuth().getJwtSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT 签名密钥不能为空");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT 签名密钥的 UTF-8 编码长度不能少于32字节");
        }
        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }
}
