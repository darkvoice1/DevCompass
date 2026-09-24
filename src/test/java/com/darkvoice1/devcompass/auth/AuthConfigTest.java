package com.darkvoice1.devcompass.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;

import com.darkvoice1.devcompass.auth.service.UserAccountService;
import com.darkvoice1.devcompass.common.time.DevCompassProperties;

/**
 * 验证初始账号的启动配置规则。
 */
class AuthConfigTest {

    /**
     * 验证未配置初始账号时不会创建用户。
     */
    @Test
    void shouldSkipInitializationWhenCredentialsAreNotConfigured() throws Exception {
        UserAccountService service = mock(UserAccountService.class);
        ApplicationRunner runner = new AuthConfig().initialUserInitializer(
                service, new DevCompassProperties());

        runner.run(null);

        verify(service, never()).initializeFirstUser(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    /**
     * 验证初始用户名和密码必须同时配置。
     */
    @Test
    void shouldRejectIncompleteInitialCredentials() {
        UserAccountService service = mock(UserAccountService.class);
        DevCompassProperties properties = new DevCompassProperties();
        properties.getAuth().setInitialUsername("admin");
        ApplicationRunner runner = new AuthConfig().initialUserInitializer(service, properties);

        assertThatThrownBy(() -> runner.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("初始用户名和初始密码必须同时配置");
    }

    /**
     * 验证配置完整时会交给用户服务初始化。
     */
    @Test
    void shouldInitializeConfiguredUser() throws Exception {
        UserAccountService service = mock(UserAccountService.class);
        DevCompassProperties properties = new DevCompassProperties();
        properties.getAuth().setInitialUsername("admin");
        properties.getAuth().setInitialPassword("安全密码123");
        ApplicationRunner runner = new AuthConfig().initialUserInitializer(service, properties);

        runner.run(null);

        verify(service).initializeFirstUser("admin", "安全密码123");
    }
}
