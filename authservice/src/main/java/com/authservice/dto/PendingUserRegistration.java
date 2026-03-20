package com.authservice.dto;

import lombok.Data;

@Data
public class PendingUserRegistration {
    private String id; // Will be set when saving
    private String role;
    private String email;
    private String password; // Will be encrypted
    private String companyName;
    private String companyEmail;
    private String companyNumber;
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
    private String submittedAt;
}