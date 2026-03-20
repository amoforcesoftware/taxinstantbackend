package com.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${admin.email}")
    private String adminEmail;

    public void sendRegistrationRequestToAdmin(Map<String, Object> user) {
        try {
            log.info("Sending registration request email to admin for user: {}", user.get("email"));

            Context context = new Context();
            context.setVariable("user", user);

            String htmlContent = templateEngine.process("admin-registration-request", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(adminEmail);
            helper.setSubject("New Registration Request - " + user.get("role"));
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Registration request email sent successfully to admin");
        } catch (Exception e) {
            log.error("Failed to send email to admin: {}", e.getMessage());
            throw new RuntimeException("Failed to send email to admin", e);
        }
    }

    public void sendApprovalEmail(String userEmail, String role) {
        try {
            log.info("Sending approval email to user: {}", userEmail);

            Context context = new Context();
            context.setVariable("role", role);

            String htmlContent = templateEngine.process("user-approval", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(userEmail);
            helper.setSubject("Registration Approved - TaxInstant");
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Approval email sent successfully to: {}", userEmail);
        } catch (Exception e) {
            log.error("Failed to send approval email: {}", e.getMessage());
            throw new RuntimeException("Failed to send approval email", e);
        }
    }

    public void sendRejectionEmail(String userEmail, String role, String reason) {
        try {
            log.info("Sending rejection email to user: {}", userEmail);

            Context context = new Context();
            context.setVariable("role", role);
            context.setVariable("reason", reason != null ? reason : "Does not meet our requirements");

            String htmlContent = templateEngine.process("user-rejection", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(userEmail);
            helper.setSubject("Registration Update - TaxInstant");
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Rejection email sent successfully to: {}", userEmail);
        } catch (Exception e) {
            log.error("Failed to send rejection email: {}", e.getMessage());
            throw new RuntimeException("Failed to send rejection email", e);
        }
    }

    public void sendPasswordResetEmail(String userEmail, String token) {
        try {
            log.info("Sending password reset email to: {}", userEmail);

            String resetLink = "http://localhost:5173/reset-password?token=" + token;

            Context context = new Context();
            context.setVariable("resetLink", resetLink);
            context.setVariable("expiryMinutes", 10);

            String htmlContent = templateEngine.process("password-reset", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(userEmail);
            helper.setSubject("Reset Your Password - TaxInstant");
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Password reset email sent successfully to: {}", userEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email: {}", e.getMessage());
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}