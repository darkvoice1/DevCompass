package com.darkvoice1.devcompass.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

import com.darkvoice1.devcompass.auth.entity.UserAccount;
import com.darkvoice1.devcompass.auth.repository.UserAccountMapper;
import com.darkvoice1.devcompass.auth.service.UserAccountService;

/**
 * 验证用户创建、密码编码和初始账号规则。
 */
class UserAccountServiceTest {

    private UserAccountMapper userAccountMapper;
    private UserAccountService userAccountService;

    /**
     * 使用真实密码编码器和模拟数据访问对象初始化测试。
     */
    @BeforeEach
    void setUp() {
        userAccountMapper = mock(UserAccountMapper.class);
        userAccountService = new UserAccountService(
                userAccountMapper, PasswordEncoderFactories.createDelegatingPasswordEncoder());
    }

    /**
     * 验证创建用户时用户名会规范化，数据库只接收 BCrypt 摘要。
     */
    @Test
    void shouldNormalizeUsernameAndStorePasswordHash() {
        UserAccount first = userAccountService.createUser("  Admin  ", "安全密码123");
        userAccountService.createUser("admin2", "安全密码123");

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        UserAccount storedFirst = captor.getAllValues().get(0);
        UserAccount storedSecond = captor.getAllValues().get(1);

        assertThat(first).isSameAs(storedFirst);
        assertThat(storedFirst.getUsername()).isEqualTo("admin");
        assertThat(storedFirst.getPasswordHash()).startsWith("{bcrypt}");
        assertThat(storedFirst.getPasswordHash()).doesNotContain("安全密码123");
        assertThat(storedFirst.getEnabled()).isTrue();
        assertThat(storedSecond.getPasswordHash()).isNotEqualTo(storedFirst.getPasswordHash());
        assertThat(userAccountService.matchesPassword(storedFirst, "安全密码123")).isTrue();
        assertThat(userAccountService.matchesPassword(storedFirst, "错误密码")).isFalse();
    }

    /**
     * 验证用户名和密码的安全边界。
     */
    @Test
    void shouldRejectInvalidCredentialsBeforeInsert() {
        assertThatThrownBy(() -> userAccountService.createUser("  ", "安全密码123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("用户名不能为空");
        assertThatThrownBy(() -> userAccountService.createUser("admin", "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("密码不能为空");
        assertThatThrownBy(() -> userAccountService.createUser("admin", "密".repeat(25)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("密码的 UTF-8 编码长度不能超过72字节");
        verify(userAccountMapper, never()).insert(any(UserAccount.class));
    }

    /**
     * 验证用户表为空时创建首个账号，已有账号时保持不变。
     */
    @Test
    void shouldOnlyInitializeUserWhenTableIsEmpty() {
        when(userAccountMapper.countAll()).thenReturn(0L, 1L);

        assertThat(userAccountService.initializeFirstUser("admin", "安全密码123")).isTrue();
        assertThat(userAccountService.initializeFirstUser("other", "其他密码123")).isFalse();

        verify(userAccountMapper, org.mockito.Mockito.times(1)).insert(any(UserAccount.class));
    }

    /**
     * 验证用户名查询不区分大小写和首尾空格。
     */
    @Test
    void shouldFindUserByNormalizedUsername() {
        UserAccount user = new UserAccount();
        user.setUsername("admin");
        when(userAccountMapper.selectByUsername("admin")).thenReturn(user);

        assertThat(userAccountService.findByUsername("  ADMIN ")).isSameAs(user);
        assertThat(userAccountService.findByUsername("  ")).isNull();
        verify(userAccountMapper).selectByUsername("admin");
    }
}
