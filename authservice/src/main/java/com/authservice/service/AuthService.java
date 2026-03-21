package com.authservice.service;

import com.authservice.dto.*;
import com.authservice.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final WebClient webClient;
    private final SupabaseStorageService storageService;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${admin.email}")
    private String adminEmail;

    // ================= REGISTER (PENDING APPROVAL) =================

    public void registerPending(
            String role,
            String email,
            String password,
            String companyName,
            String companyEmail,
            String companyNumber,
            MultipartFile form16,
            MultipartFile incorporationDocument,
            MultipartFile financialDocument,
            MultipartFile balanceIncomeStatement,
            MultipartFile balanceSheet,
            MultipartFile incomeStatement,
            MultipartFile soleDoc1,
            MultipartFile soleDoc2,
            MultipartFile soleDoc3) {

        log.info("Processing pending registration for role: {}, email: {}", role, email);

        // ✅ ROLE VALIDATION
        validateRole(role,
                companyName,
                companyNumber,
                form16,
                incorporationDocument,
                financialDocument,
                balanceIncomeStatement,
                balanceSheet,
                incomeStatement,
                soleDoc1,
                soleDoc2,
                soleDoc3);

        // Check if user already exists in pending or approved
        checkIfUserExists(email, role);

        // Upload documents first to get URLs
        Map<String, String> documentUrls = uploadDocumentsAndGetUrls(
                role,
                form16,
                incorporationDocument,
                financialDocument,
                balanceIncomeStatement,
                balanceSheet,
                incomeStatement,
                soleDoc1,
                soleDoc2,
                soleDoc3);

        // Create pending user record in Supabase
        Map<String, Object> pendingUser = new HashMap<>();
        pendingUser.put("email", email);
        pendingUser.put("password_hash", encoder.encode(password));
        pendingUser.put("role", role);
        pendingUser.put("company_name", companyName);
        pendingUser.put("company_email", companyEmail);
        pendingUser.put("company_number", companyNumber);

        // Document URLs
        pendingUser.put("form16_url", documentUrls.get("FORM16"));
        pendingUser.put("incorporation_document_url", documentUrls.get("INCORPORATION"));
        pendingUser.put("financial_document_url", documentUrls.get("FINANCIAL_DOCUMENT"));
        pendingUser.put("balance_income_statement_url", documentUrls.get("BALANCE_INCOME"));
        pendingUser.put("balance_sheet_url", documentUrls.get("BALANCE_SHEET"));
        pendingUser.put("income_statement_url", documentUrls.get("INCOME_STATEMENT"));
        pendingUser.put("sole_doc1_url", documentUrls.get("SOLE_DOC_1"));
        pendingUser.put("sole_doc2_url", documentUrls.get("SOLE_DOC_2"));
        pendingUser.put("sole_doc3_url", documentUrls.get("SOLE_DOC_3"));

        pendingUser.put("status", "PENDING");
        pendingUser.put("submitted_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        // Save to pending_users table in Supabase
        Map<String, Object> savedUser = webClient.post()
                .uri("/rest/v1/pending_users")
                .bodyValue(pendingUser)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        log.info("Pending user saved with ID: {}", savedUser != null ? savedUser.get("id") : "unknown");

        // Get the complete saved user with ID
        List<Map<String, Object>> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/v1/pending_users")
                        .queryParam("email", "eq." + email)
                        .queryParam("role", "eq." + role)
                        .queryParam("order", "submitted_at.desc")
                        .queryParam("limit", "1")
                        .build())
                .retrieve()
                .bodyToMono(List.class)
                .block();

        if (response != null && !response.isEmpty()) {
            Map<String, Object> savedPendingUser = response.get(0);
            // Send email to admin with registration details
            // emailService.sendRegistrationRequestToAdmin(savedPendingUser);
            log.info("Registration request email sent to admin for user: {}", email);
        }
    }

    // ================= ADMIN REGISTRATION =================

    public void registerAdmin(AdminRegistrationRequest request) {
        log.info("Registering admin with email: {}", request.getEmail());

        // Check if admin already exists
        List<Map<String, Object>> existingAdmins = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/v1/users")
                        .queryParam("email", "eq." + request.getEmail())
                        .queryParam("role", "eq.ADMIN")
                        .build())
                .retrieve()
                .bodyToMono(List.class)
                .block();

        if (existingAdmins != null && !existingAdmins.isEmpty()) {
            throw new RuntimeException("Admin already exists with this email");
        }

        // Create admin user
        Map<String, Object> admin = new HashMap<>();
        admin.put("email", request.getEmail());
        admin.put("password_hash", encoder.encode(request.getPassword()));
        admin.put("role", "ADMIN");
        admin.put("created_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        webClient.post()
                .uri("/rest/v1/users")
                .bodyValue(admin)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("Admin registered successfully: {}", request.getEmail());
    }

    // ================= ADMIN LOGIN =================

    public AdminLoginResponse adminLogin(LoginRequest request) {
        log.info("Admin login attempt for email: {}", request.getEmail());

        List<Map<String, Object>> users = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/v1/users")
                        .queryParam("email", "eq." + request.getEmail())
                        .queryParam("role", "eq.ADMIN")
                        .build())
                .retrieve()
                .bodyToMono(List.class)
                .block();

        if (users == null || users.isEmpty()) {
            throw new RuntimeException("Admin not found");
        }

        Map<String, Object> user = users.get(0);
        String passwordHash = user.get("password_hash").toString();

        if (!encoder.matches(request.getPassword(), passwordHash)) {
            throw new RuntimeException("Invalid password");
        }

        String accessToken = jwtUtil.generateToken(
                request.getEmail(),
                "ADMIN",
                900000); // 15 minutes

        String refreshToken = jwtUtil.generateToken(
                request.getEmail(),
                "ADMIN",
                604800000); // 7 days

        AdminLoginResponse response = new AdminLoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setEmail(request.getEmail());
        response.setRole("ADMIN");

        log.info("Admin login successful: {}", request.getEmail());
        return response;
    }

    // ================= USER LOGIN (FOR APPROVED USERS) =================

    public LoginResponse login(LoginRequest request) {
        log.info("User login attempt for email: {}, role: {}", request.getEmail(), request.getRole());

        List<Map<String, Object>> users = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/v1/users")
                        .queryParam("email", "eq." + request.getEmail())
                        .queryParam("role", "eq." + request.getRole())
                        .build())
                .retrieve()
                .bodyToMono(List.class)
                .block();

        if (users == null || users.isEmpty()) {
            throw new RuntimeException("User not found or not approved yet");
        }

        Map<String, Object> user = users.get(0);
        String passwordHash = user.get("password_hash").toString();

        if (!encoder.matches(request.getPassword(), passwordHash)) {
            throw new RuntimeException("Invalid password");
        }

        String accessToken = jwtUtil.generateToken(
                request.getEmail(),
                request.getRole(),
                900000);

        String refreshToken = jwtUtil.generateToken(
                request.getEmail(),
                request.getRole(),
                604800000);

        log.info("User login successful: {}", request.getEmail());
        return new LoginResponse(accessToken, refreshToken);
    }

    // ================= APPROVAL PROCESS =================

    public void processApproval(ApprovalRequest request) {
        log.info("Processing approval request for pendingId: {}, approved: {}",
                request.getPendingId(), request.isApproved());

        // Get pending user
        List<Map<String, Object>> pendingUsers = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/v1/pending_users")
                        .queryParam("id", "eq." + request.getPendingId())
                        .build())
                .retrieve()
                .bodyToMono(List.class)
                .block();

        if (pendingUsers == null || pendingUsers.isEmpty()) {
            throw new RuntimeException("Pending user not found with id: " + request.getPendingId());
        }

        Map<String, Object> pendingUser = pendingUsers.get(0);

        // In processApproval method, comment out email sending:
        if (request.isApproved()) {
            // Move user to main users table
            moveToMainUsers(pendingUser);

            // Send approval email to user - COMMENTED OUT
            // emailService.sendApprovalEmail(
            // (String) pendingUser.get("email"),
            // (String) pendingUser.get("role"));
            log.info("📧 [EMAIL DISABLED] Approval email would be sent to: {}", pendingUser.get("email"));

            log.info("User approved and moved to main users: {}", pendingUser.get("email"));
        } else {
            // Send rejection email - COMMENTED OUT
            // emailService.sendRejectionEmail(
            // (String) pendingUser.get("email"),
            // (String) pendingUser.get("role"),
            // request.getRejectionReason());
            log.info("📧 [EMAIL DISABLED] Rejection email would be sent to: {}", pendingUser.get("email"));

            log.info("User rejected: {}", pendingUser.get("email"));
        }
        // Delete the pending user record after processing (both approve and reject)
        deletePendingUser(request.getPendingId());
        log.info("Pending user deleted with ID: {}", request.getPendingId());
    }

    private void deletePendingUser(UUID pendingId) {
        try {
            log.info("Deleting pending user with ID: {}", pendingId);

            webClient.delete()
                    .uri("/rest/v1/pending_users?id=eq." + pendingId)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Supabase delete error: {}", errorBody);
                                        return Mono.error(new RuntimeException("Delete error: " + errorBody));
                                    }))
                    .bodyToMono(String.class)
                    .block();

            log.info("Pending user deleted successfully: {}", pendingId);
        } catch (Exception e) {
            log.error("Failed to delete pending user {}: {}", pendingId, e.getMessage());
            // Don't throw - we don't want to fail the approval process if delete fails
            // But log it for investigation
        }
    }

    // ================= GET PENDING USERS =================

    public List<PendingUserResponse> getPendingUsers() {
        log.info("Fetching all pending users");

        List<Map<String, Object>> pendingUsers = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/v1/pending_users")
                        .queryParam("status", "eq.PENDING")
                        .queryParam("order", "submitted_at.desc")
                        .build())
                .retrieve()
                .bodyToMono(List.class)
                .block();

        List<PendingUserResponse> responses = new ArrayList<>();

        if (pendingUsers != null) {
            for (Map<String, Object> user : pendingUsers) {
                PendingUserResponse response = new PendingUserResponse();
                response.setPendingId(UUID.fromString(user.get("id").toString()));
                response.setEmail((String) user.get("email"));
                response.setRole((String) user.get("role"));
                response.setCompanyName((String) user.get("company_name"));
                response.setCompanyEmail((String) user.get("company_email"));
                response.setCompanyNumber((String) user.get("company_number"));
                response.setStatus((String) user.get("status"));
                response.setSubmittedAt((String) user.get("submitted_at"));

                // Collect document URLs
                List<String> documents = new ArrayList<>();
                if (user.get("form16_url") != null)
                    documents.add("Form16: " + user.get("form16_url"));
                if (user.get("incorporation_document_url") != null)
                    documents.add("Incorporation: " + user.get("incorporation_document_url"));
                if (user.get("financial_document_url") != null)
                    documents.add("Financial: " + user.get("financial_document_url"));
                if (user.get("balance_income_statement_url") != null)
                    documents.add("Balance Income: " + user.get("balance_income_statement_url"));
                if (user.get("balance_sheet_url") != null)
                    documents.add("Balance Sheet: " + user.get("balance_sheet_url"));
                if (user.get("income_statement_url") != null)
                    documents.add("Income Statement: " + user.get("income_statement_url"));
                if (user.get("sole_doc1_url") != null)
                    documents.add("Sole Doc 1: " + user.get("sole_doc1_url"));
                if (user.get("sole_doc2_url") != null)
                    documents.add("Sole Doc 2: " + user.get("sole_doc2_url"));
                if (user.get("sole_doc3_url") != null)
                    documents.add("Sole Doc 3: " + user.get("sole_doc3_url"));

                response.setDocuments(documents);
                responses.add(response);
            }
        }

        log.info("Found {} pending users", responses.size());
        return responses;
    }

    // ================= HELPER METHODS =================

    private void checkIfUserExists(String email, String role) {
        // Check pending users
        List<Map<String, Object>> pendingUsers = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/v1/pending_users")
                        .queryParam("email", "eq." + email)
                        .queryParam("role", "eq." + role)
                        .queryParam("status", "eq.PENDING")
                        .build())
                .retrieve()
                .bodyToMono(List.class)
                .block();

        if (pendingUsers != null && !pendingUsers.isEmpty()) {
            throw new RuntimeException("Registration already pending for this email and role");
        }

        // Check main users
        List<Map<String, Object>> users = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/v1/users")
                        .queryParam("email", "eq." + email)
                        .queryParam("role", "eq." + role)
                        .build())
                .retrieve()
                .bodyToMono(List.class)
                .block();

        if (users != null && !users.isEmpty()) {
            throw new RuntimeException("User already exists with this email and role");
        }
    }

    private Map<String, String> uploadDocumentsAndGetUrls(
            String role,
            MultipartFile form16,
            MultipartFile incorporationDocument,
            MultipartFile financialDocument,
            MultipartFile balanceIncomeStatement,
            MultipartFile balanceSheet,
            MultipartFile incomeStatement,
            MultipartFile soleDoc1,
            MultipartFile soleDoc2,
            MultipartFile soleDoc3) {

        Map<String, String> urls = new HashMap<>();

        urls.put("FORM16", uploadAndGetUrl(form16, "salaried/form16"));
        urls.put("INCORPORATION", uploadAndGetUrl(incorporationDocument,
                role.equals("STARTUP_SME") ? "startup/incorporation" : "corporate/incorporation"));
        urls.put("FINANCIAL_DOCUMENT", uploadAndGetUrl(financialDocument, "startup/financial"));
        urls.put("BALANCE_INCOME", uploadAndGetUrl(balanceIncomeStatement, "startup/balance-income"));
        urls.put("BALANCE_SHEET", uploadAndGetUrl(balanceSheet, "corporate/balance-sheet"));
        urls.put("INCOME_STATEMENT", uploadAndGetUrl(incomeStatement, "corporate/income-statement"));
        urls.put("SOLE_DOC_1", uploadAndGetUrl(soleDoc1, "corporate/sole"));
        urls.put("SOLE_DOC_2", uploadAndGetUrl(soleDoc2, "corporate/sole"));
        urls.put("SOLE_DOC_3", uploadAndGetUrl(soleDoc3, "corporate/sole"));

        return urls;
    }

    private String uploadAndGetUrl(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return storageService.uploadFile(file, folder);
    }

    private void moveToMainUsers(Map<String, Object> pendingUser) {
        try {
            Map<String, Object> user = new HashMap<>();

            // Required fields
            user.put("email", pendingUser.get("email"));
            user.put("password_hash", pendingUser.get("password_hash"));
            user.put("role", pendingUser.get("role"));

            // Optional fields - only add if not null
            if (pendingUser.get("company_name") != null && !((String) pendingUser.get("company_name")).isEmpty()) {
                user.put("company_name", pendingUser.get("company_name"));
            }
            if (pendingUser.get("company_email") != null && !((String) pendingUser.get("company_email")).isEmpty()) {
                user.put("company_email", pendingUser.get("company_email"));
            }
            if (pendingUser.get("company_number") != null && !((String) pendingUser.get("company_number")).isEmpty()) {
                user.put("company_number", pendingUser.get("company_number"));
            }

            // created_at will be set by default now()
            // No need to explicitly set it

            log.info("Attempting to save user to Supabase: {}", user);

            // Save to main users table
            String response = webClient.post()
                    .uri("/rest/v1/users")
                    .bodyValue(user)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Supabase error response: {}", errorBody);
                                        return Mono.error(new RuntimeException("Supabase error: " + errorBody));
                                    }))
                    .bodyToMono(String.class)
                    .block();

            log.info("User saved successfully to main table. Response: {}", response);

            // Get the inserted user's ID
            List<Map<String, Object>> userResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/rest/v1/users")
                            .queryParam("email", "eq." + pendingUser.get("email"))
                            .queryParam("role", "eq." + pendingUser.get("role"))
                            .queryParam("select", "id")
                            .build())
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            if (userResponse != null && !userResponse.isEmpty()) {
                Object idObj = userResponse.get(0).get("id");
                if (idObj != null) {
                    UUID userId = UUID.fromString(idObj.toString());
                    log.info("Retrieved user ID: {}", userId);

                    // Move documents to documents table
                    moveDocumentsToMain(userId, pendingUser);
                } else {
                    log.error("User ID is null in response");
                }
            } else {
                log.error("Could not retrieve user after insertion. Response: {}", userResponse);
            }

        } catch (Exception e) {
            log.error("Error in moveToMainUsers: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to move user to main table: " + e.getMessage());
        }
    }

    private void moveDocumentsToMain(UUID userId, Map<String, Object> pendingUser) {
        saveDocumentRecord(userId, (String) pendingUser.get("form16_url"), "FORM16");
        saveDocumentRecord(userId, (String) pendingUser.get("incorporation_document_url"), "INCORPORATION");
        saveDocumentRecord(userId, (String) pendingUser.get("financial_document_url"), "FINANCIAL_DOCUMENT");
        saveDocumentRecord(userId, (String) pendingUser.get("balance_income_statement_url"), "BALANCE_INCOME");
        saveDocumentRecord(userId, (String) pendingUser.get("balance_sheet_url"), "BALANCE_SHEET");
        saveDocumentRecord(userId, (String) pendingUser.get("income_statement_url"), "INCOME_STATEMENT");
        saveDocumentRecord(userId, (String) pendingUser.get("sole_doc1_url"), "SOLE_DOC_1");
        saveDocumentRecord(userId, (String) pendingUser.get("sole_doc2_url"), "SOLE_DOC_2");
        saveDocumentRecord(userId, (String) pendingUser.get("sole_doc3_url"), "SOLE_DOC_3");
    }

    private void saveDocumentRecord(UUID userId, String fileUrl, String documentType) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return;
        }

        try {
            Map<String, Object> document = new HashMap<>();
            document.put("user_id", userId);
            document.put("document_type", documentType);
            document.put("file_url", fileUrl);
            // created_at will be set by default now()

            log.info("Saving document: {} for user: {}", documentType, userId);

            webClient.post()
                    .uri("/rest/v1/documents")
                    .bodyValue(document)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Supabase document error: {}", errorBody);
                                        return Mono.error(new RuntimeException("Document error: " + errorBody));
                                    }))
                    .bodyToMono(String.class)
                    .block();

            log.info("Document saved successfully: {} for user: {}", documentType, userId);
        } catch (Exception e) {
            log.error("Failed to save document {}: {}", documentType, e.getMessage());
            // Don't throw - continue with other documents
        }
    }

    // ================= ROLE VALIDATION =================

    private void validateRole(
            String role,
            String companyName,
            String companyNumber,
            MultipartFile form16,
            MultipartFile incorporationDocument,
            MultipartFile financialDocument,
            MultipartFile balanceIncomeStatement,
            MultipartFile balanceSheet,
            MultipartFile incomeStatement,
            MultipartFile soleDoc1,
            MultipartFile soleDoc2,
            MultipartFile soleDoc3) {

        switch (role) {

            case "INDIVIDUAL":
                if (companyName == null || companyNumber == null)
                    throw new RuntimeException("Company name and number required");
                break;

            case "SALARIED_PROFESSIONAL":
                if (form16 == null || form16.isEmpty())
                    throw new RuntimeException("Form16 required");
                break;

            case "STARTUP_SME":
                if (incorporationDocument == null || incorporationDocument.isEmpty() ||
                        financialDocument == null || financialDocument.isEmpty() ||
                        balanceIncomeStatement == null || balanceIncomeStatement.isEmpty())
                    throw new RuntimeException("All 3 startup documents required");
                break;

            case "CORPORATE":
            case "ENTERPRISE":
            case "INDUSTRIES":
                if (incorporationDocument == null || incorporationDocument.isEmpty() ||
                        balanceSheet == null || balanceSheet.isEmpty() ||
                        incomeStatement == null || incomeStatement.isEmpty() ||
                        soleDoc1 == null || soleDoc1.isEmpty() ||
                        soleDoc2 == null || soleDoc2.isEmpty() ||
                        soleDoc3 == null || soleDoc3.isEmpty())
                    throw new RuntimeException("All corporate documents required");
                break;

            default:
                throw new RuntimeException("Invalid role");
        }
    }
    // ================= FORGOT PASSWORD =================

    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Processing forgot password request for email: {}", request.getEmail());

        // Check if user exists in main users table
        List<Map<String, Object>> users = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/v1/users")
                        .queryParam("email", "eq." + request.getEmail())
                        .build())
                .retrieve()
                .bodyToMono(List.class)
                .block();

        if (users == null || users.isEmpty()) {
            // Don't reveal if email exists or not for security
            log.info("Password reset requested for non-existent email: {}", request.getEmail());
            return;
        }

        Map<String, Object> user = users.get(0);
        String userId = user.get("id").toString();
        String role = (String) user.get("role");

        // Generate reset token (expires in 10 minutes)
        String resetToken = jwtUtil.generateToken(
                request.getEmail(),
                role,
                600000 // 10 minutes in milliseconds
        );

        // Store reset token in database (you'll need a password_resets table)
        savePasswordResetToken(userId, resetToken);

        // Send reset email
        emailService.sendPasswordResetEmail(request.getEmail(), resetToken);

        log.info("Password reset email sent to: {}", request.getEmail());
    }

    // ================= RESET PASSWORD =================

    public void resetPassword(ResetPasswordRequest request) {
        log.info("Processing password reset with token");

        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        // Validate token and get user info
        Map<String, Object> tokenInfo = validateResetToken(request.getToken());

        if (tokenInfo == null) {
            throw new RuntimeException("Invalid or expired reset token");
        }

        String email = (String) tokenInfo.get("email");
        String role = (String) tokenInfo.get("role");

        // Update password in users table
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("password_hash", encoder.encode(request.getNewPassword()));

        webClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/v1/users")
                        .queryParam("email", "eq." + email)
                        .queryParam("role", "eq." + role)
                        .build())
                .bodyValue(updateData)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // Delete used token
        deletePasswordResetToken(request.getToken());

        log.info("Password reset successful for email: {}", email);
    }

    // ================= TOKEN VALIDATION =================

    private Map<String, Object> validateResetToken(String token) {
        try {
            // Validate JWT token
            if (!jwtUtil.validateToken(token)) {
                return null;
            }

            // Extract claims
            String email = jwtUtil.extractEmail(token);
            String role = jwtUtil.extractRole(token);

            // Check if token exists in database and is not used
            List<Map<String, Object>> tokens = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/rest/v1/password_resets")
                            .queryParam("token", "eq." + token)
                            .queryParam("used", "eq.false")
                            .queryParam("expires_at",
                                    "gt." + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                            .build())
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            if (tokens == null || tokens.isEmpty()) {
                return null;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("email", email);
            result.put("role", role);
            return result;

        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return null;
        }
    }

    private void savePasswordResetToken(String userId, String token) {
        Map<String, Object> resetRecord = new HashMap<>();
        resetRecord.put("user_id", userId);
        resetRecord.put("token", token);
        resetRecord.put("expires_at", LocalDateTime.now().plusMinutes(10).format(DateTimeFormatter.ISO_DATE_TIME));
        resetRecord.put("used", false);
        resetRecord.put("created_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        webClient.post()
                .uri("/rest/v1/password_resets")
                .bodyValue(resetRecord)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private void deletePasswordResetToken(String token) {
        webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/v1/password_resets")
                        .queryParam("token", "eq." + token)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}