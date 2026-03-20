package com.authservice.dto;

import lombok.Data;

@Data
public class ForgotPasswordRequest {
    private String email;
}