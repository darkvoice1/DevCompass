package com.darkvoice1.devcompass.auth;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import com.darkvoice1.devcompass.auth.service.UserAccountService;
import com.darkvoice1.devcompass.common.time.DevCompassProperties;

/**
 * 提供密码编码器和初始账号创建规则。
 */
@Configuration
public class AuthConfig {

    /**
     * 创建支持未来算法升级的密码编码器，当前新密码使用 BCrypt。
     *
     * @return 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * 应用启动时按配置创建首个账号。
     *
     * @param userAccountService 用户账号服务
     * @param properties 应用自定义配置
     * @return 初始账号创建任务
     */
    @Bean
    public ApplicationRunner initialUserInitializer(
            UserAccountService userAccountService, DevCompassProperties properties) {
        return arguments -> {
            DevCompassProperties.Auth auth = properties.getAuth();
            if (auth == null) {
                return;
            }

            boolean hasUsername = StringUtils.hasText(auth.getInitialUsername());
            boolean hasPassword = StringUtils.hasText(auth.getInitialPassword());
            if (!hasUsername && !hasPassword) {
                return;
            }
            if (!hasUsername || !hasPassword) {
                throw new IllegalStateException("初始用户名和初始密码必须同时配置");
            }

            userAccountService.initializeFirstUser(
                    auth.getInitialUsername(), auth.getInitialPassword());
        };
    }
}
