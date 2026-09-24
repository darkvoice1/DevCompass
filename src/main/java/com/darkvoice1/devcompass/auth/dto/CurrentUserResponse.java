package com.darkvoice1.devcompass.auth.dto;

/**
 * 当前登录用户信息。
 */
public class CurrentUserResponse {

    private Long id;

    private String username;

    public CurrentUserResponse(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
