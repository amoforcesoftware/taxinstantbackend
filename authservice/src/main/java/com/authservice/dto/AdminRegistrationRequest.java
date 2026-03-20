// DTOs for admin registration and approval
package com.authservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class AdminRegistrationRequest {
    private String email;
    private String password;
    private String role = "ADMIN"; // Fixed role for admin
}