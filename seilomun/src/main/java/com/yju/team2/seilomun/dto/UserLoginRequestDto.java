package com.yju.team2.seilomun.dto;

import com.yju.team2.seilomun.validation.ValidPassword;

public class UserLoginRequestDto {

    private String username;
    @ValidPassword
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
