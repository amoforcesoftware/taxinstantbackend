// PendingUser entity/model
package com.authservice.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PendingUser {
    private UUID id;
    private String email;
    private String passwordHash;
    private String role;
    private String companyName;
    private String companyEmail;
    private String companyNumber;

    // Document URLs
    private String form16Url;
    private String incorporationDocumentUrl;
    private String financialDocumentUrl;
    private String balanceIncomeStatementUrl;
    private String balanceSheetUrl;
    private String incomeStatementUrl;
    private String soleDoc1Url;
    private String soleDoc2Url;
    private String soleDoc3Url;

    private String status; // PENDING, APPROVED, REJECTED
    private LocalDateTime submittedAt;
}