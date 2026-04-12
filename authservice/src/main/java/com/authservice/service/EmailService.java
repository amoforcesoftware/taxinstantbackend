// EmailService.java
package com.authservice.service;

import com.authservice.client.EmailServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailServiceClient emailClient;

    @Value("${admin.email}")
    private String adminEmail;

    public void sendRegistrationRequestToAdmin(Map<String, Object> user) {
        try {
            log.info("Sending registration request email to admin for user: {}", user.get("email"));

            boolean success = emailClient.sendRegistrationRequestToAdmin(user, adminEmail);

            if (success) {
                log.info("Registration request email sent successfully to admin");
            } else {
                log.error("Failed to send registration request email to admin");
            }
        } catch (Exception e) {
            log.error("Error sending registration request email: {}", e.getMessage());
        }
    }

    public void sendApprovalEmail(String userEmail, String role) {
        try {
            log.info("Sending approval email to user: {}", userEmail);

            boolean success = emailClient.sendApprovalEmail(userEmail, role);

            if (success) {
                log.info("Approval email sent successfully to: {}", userEmail);
            } else {
                log.error("Failed to send approval email to: {}", userEmail);
            }
        } catch (Exception e) {
            log.error("Error sending approval email: {}", e.getMessage());
        }
    }

    public void sendRejectionEmail(String userEmail, String role, String reason) {
        try {
            log.info("Sending rejection email to user: {}", userEmail);

            boolean success = emailClient.sendRejectionEmail(userEmail, role, reason);

            if (success) {
                log.info("Rejection email sent successfully to: {}", userEmail);
            } else {
                log.error("Failed to send rejection email to: {}", userEmail);
            }
        } catch (Exception e) {
            log.error("Error sending rejection email: {}", e.getMessage());
        }
    }

    public void sendPasswordResetEmail(String userEmail, String token) {
        try {
            log.info("Sending password reset email to: {}", userEmail);

            String resetLink = "https://www.taxinstant.com/reset-password?token=" + token;
            boolean success = emailClient.sendPasswordResetEmail(userEmail, resetLink);

            if (success) {
                log.info("Password reset email sent successfully to: {}", userEmail);
            } else {
                log.error("Failed to send password reset email to: {}", userEmail);
            }
        } catch (Exception e) {
            log.error("Error sending password reset email: {}", e.getMessage());
        }
    }
}