package com.authservice.dto;

import lombok.Data;

@Data
public class AdminLoginResponse {
    private String accessToken;
    private String refreshToken;
    private String email;
    private String role;
}