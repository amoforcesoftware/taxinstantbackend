// Updated Controller
package com.authservice.controller;

import com.authservice.service.AuthService;
import com.authservice.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ================= PUBLIC ENDPOINTS =================

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestParam String role,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String companyEmail,
            @RequestParam(required = false) String companyNumber,
            @RequestParam(required = false) MultipartFile form16,
            @RequestParam(required = false) MultipartFile incorporationDocument,
            @RequestParam(required = false) MultipartFile financialDocument,
            @RequestParam(required = false) MultipartFile balanceIncomeStatement,
            @RequestParam(required = false) MultipartFile balanceSheet,
            @RequestParam(required = false) MultipartFile incomeStatement,
            @RequestParam(required = false) MultipartFile soleDoc1,
            @RequestParam(required = false) MultipartFile soleDoc2,
            @RequestParam(required = false) MultipartFile soleDoc3) {

        authService.registerPending(
                role, email, password,
                companyName, companyEmail, companyNumber,
                form16, incorporationDocument, financialDocument,
                balanceIncomeStatement, balanceSheet, incomeStatement,
                soleDoc1, soleDoc2, soleDoc3);

        return ResponseEntity
                .ok("Registration request submitted successfully. You will receive an email once approved.");
    }

    // ================= LOGIN ENDPOINT =================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    // ================= ADMIN ENDPOINTS =================

    @PostMapping("/admin/register")
    public ResponseEntity<?> registerAdmin(@RequestBody AdminRegistrationRequest request) {
        authService.registerAdmin(request);
        return ResponseEntity.ok("Admin registered successfully");
    }

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.adminLogin(request));
    }

    @GetMapping("/admin/pending-users")
    public ResponseEntity<List<PendingUserResponse>> getPendingUsers() {
        return ResponseEntity.ok(authService.getPendingUsers());
    }

    @PostMapping("/admin/process-approval")
    public ResponseEntity<?> processApproval(@RequestBody ApprovalRequest request) {
        authService.processApproval(request);
        return ResponseEntity.ok("Approval processed successfully");
    }
    // ================= FORGOT PASSWORD ENDPOINTS =================

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            authService.forgotPassword(request);
            return ResponseEntity.ok("If your email exists in our system, you will receive a password reset link.");
        } catch (Exception e) {

            return ResponseEntity.status(500).body("An error occurred. Please try again later.");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request);
            return ResponseEntity.ok("Password reset successfully. You can now login with your new password.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {

            return ResponseEntity.status(500).body("An error occurred. Please try again later.");
        }
    }
    // ================= PENDING COUNT ENDPOINT =================

    @GetMapping("/admin/pending/count")
    public ResponseEntity<?> getPendingCount() {
        try {
            long count = authService.getPendingCount();
            java.util.Map<String, Long> response = new java.util.HashMap<>();
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch pending count");
        }
    }
}