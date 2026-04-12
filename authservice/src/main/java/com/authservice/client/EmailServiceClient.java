// EmailServiceClient.java
package com.authservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class EmailServiceClient {

    private final WebClient webClient;

    @Value("${email.service.url:https://taxinstant.pythonanywhere.com}")
    private String emailServiceUrl;

    public EmailServiceClient() {
        this.webClient = WebClient.builder().build();
    }

    public boolean sendEmail(String toEmail, String subject, String body) {
        try {
            Map<String, String> request = new HashMap<>();
            request.put("to_email", toEmail);
            request.put("subject", subject);
            request.put("body", body);

            Map response = webClient.post()
                    .uri(emailServiceUrl + "/send-email")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            boolean success = response != null && Boolean.TRUE.equals(response.get("success"));
            if (success) {
                log.info("Email sent successfully to: {}", toEmail);
            } else {
                log.error("Failed to send email to: {}", toEmail);
            }
            return success;

        } catch (Exception e) {
            log.error("Error sending email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    public boolean sendTemplatedEmail(String type, Map<String, Object> params) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("type", type);
            request.putAll(params);

            Map response = webClient.post()
                    .uri(emailServiceUrl + "/send-template-email")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            boolean success = response != null && Boolean.TRUE.equals(response.get("success"));
            if (success) {
                log.info("Templated email ({}) sent successfully", type);
            } else {
                log.error("Failed to send templated email ({})", type);
            }
            return success;

        } catch (Exception e) {
            log.error("Error sending templated email ({}): {}", type, e.getMessage());
            return false;
        }
    }

    public boolean sendRegistrationRequestToAdmin(Map<String, Object> user, String adminEmail) {
        Map<String, Object> params = new HashMap<>();
        params.put("user", user);
        params.put("admin_email", adminEmail);

        return sendTemplatedEmail("registration", params);
    }

    public boolean sendApprovalEmail(String email, String role) {
        Map<String, Object> params = new HashMap<>();
        params.put("to_email", email);
        params.put("role", role);

        return sendTemplatedEmail("approval", params);
    }

    public boolean sendRejectionEmail(String email, String role, String reason) {
        Map<String, Object> params = new HashMap<>();
        params.put("to_email", email);
        params.put("role", role);
        params.put("reason", reason);

        return sendTemplatedEmail("rejection", params);
    }

    public boolean sendPasswordResetEmail(String email, String resetLink) {
        Map<String, Object> params = new HashMap<>();
        params.put("to_email", email);
        params.put("reset_link", resetLink);
        params.put("expiry_minutes", 10);

        return sendTemplatedEmail("password_reset", params);
    }

    public boolean testEmailConfiguration(String testEmail) {
        try {
            Map<String, String> request = new HashMap<>();
            request.put("test_email", testEmail);

            Map response = webClient.post()
                    .uri(emailServiceUrl + "/test-email")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return response != null && Boolean.TRUE.equals(response.get("success"));

        } catch (Exception e) {
            log.error("Test email failed: {}", e.getMessage());
            return false;
        }
    }
}