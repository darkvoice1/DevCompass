package com.darkvoice1.devcompass.auth.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.darkvoice1.devcompass.auth.entity.UserAccount;

/**
 * 把项目用户转换为 Spring Security 可识别的登录用户。
 */
@Service
public class AuthUserDetailsService implements UserDetailsService {

    private final UserAccountService userAccountService;

    public AuthUserDetailsService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    /**
     * 按用户名加载登录用户。
     *
     * @param username 用户名
     * @return Spring Security 登录用户
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = userAccountService.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .disabled(!Boolean.TRUE.equals(user.getEnabled()))
                .authorities("USER")
                .build();
    }
}
