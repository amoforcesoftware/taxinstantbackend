package com.authservice.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class ApprovalRequest {
    private UUID pendingId;
    private boolean approved;
    private String rejectionReason;
}