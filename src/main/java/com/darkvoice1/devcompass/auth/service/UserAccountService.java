package com.darkvoice1.devcompass.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.darkvoice1.devcompass.auth.entity.UserAccount;
import com.darkvoice1.devcompass.auth.repository.UserAccountMapper;

/**
 * 管理用户账号和密码安全存储。
 */
@Service
public class UserAccountService {

    private static final int MAX_USERNAME_LENGTH = 100;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountMapper userAccountMapper, PasswordEncoder passwordEncoder) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 创建用户并只保存不可逆的密码摘要。
     *
     * @param username 用户名
     * @param rawPassword 原始密码
     * @return 已创建的用户
     */
    public UserAccount createUser(String username, String rawPassword) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(rawPassword);

        UserAccount user = new UserAccount();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setEnabled(true);
        userAccountMapper.insert(user);
        return user;
    }

    /**
     * 用户表为空时创建首个账号，已有账号时不重复创建。
     *
     * @param username 初始用户名
     * @param rawPassword 初始原始密码
     * @return 是否创建了初始用户
     */
    @Transactional
    public boolean initializeFirstUser(String username, String rawPassword) {
        if (userAccountMapper.countAll() > 0) {
            return false;
        }
        createUser(username, rawPassword);
        return true;
    }

    /**
     * 按用户名查询账号，用户名不区分大小写。
     *
     * @param username 用户名
     * @return 用户账号，不存在或用户名为空时返回空
     */
    public UserAccount findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return userAccountMapper.selectByUsername(username.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * 使用安全编码器核对原始密码。
     *
     * @param user 用户账号
     * @param rawPassword 待核对的原始密码
     * @return 密码是否匹配
     */
    public boolean matchesPassword(UserAccount user, String rawPassword) {
        return user != null && rawPassword != null
                && passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        if (normalizedUsername.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("用户名长度不能超过100个字符");
        }
        return normalizedUsername;
    }

    private void validatePassword(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new IllegalArgumentException("密码的 UTF-8 编码长度不能超过72字节");
        }
    }
}
