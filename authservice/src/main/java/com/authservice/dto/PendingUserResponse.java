package com.authservice.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PendingUserResponse {
    private UUID pendingId;
    private String email;
    private String role;
    private String companyName;
    private String companyEmail;
    private String companyNumber;
    private String status;
    private String submittedAt;
    private List<String> documents;
}