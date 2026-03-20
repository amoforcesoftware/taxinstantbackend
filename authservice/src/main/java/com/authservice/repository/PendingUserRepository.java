package com.authservice.repository;

import com.authservice.model.PendingUser;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PendingUserRepository {

    private final WebClient webClient;

    public PendingUser save(PendingUser user) {
        if (user.getId() == null) {
            // Insert
            Map<String, Object> userMap = convertToMap(user);

            webClient.post()
                    .uri("/rest/v1/pending_users")
                    .bodyValue(userMap)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Get the inserted user back
            List<Map<String, Object>> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/rest/v1/pending_users")
                            .queryParam("email", "eq." + user.getEmail())
                            .queryParam("role", "eq." + user.getRole())
                            .queryParam("order", "submitted_at.desc")
                            .queryParam("limit", "1")
                            .build())
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            if (response != null && !response.isEmpty()) {
                return convertToEntity(response.get(0));
            }
        } else {
            // Update
            Map<String, Object> userMap = convertToMap(user);

            webClient.patch()
                    .uri("/rest/v1/pending_users?id=eq." + user.getId())
                    .bodyValue(userMap)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return user;
        }

        return user;
    }

    public Optional<PendingUser> findById(UUID id) {
        List<Map<String, Object>> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/v1/pending_users")
                        .queryParam("id", "eq." + id)
                        .build())
                .retrieve()
                .bodyToMono(List.class)
                .block();

        if (response != null && !response.isEmpty()) {
            return Optional.of(convertToEntity(response.get(0)));
        }

        return Optional.empty();
    }

    public List<PendingUser> findByStatus(String status) {
        List<Map<String, Object>> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/v1/pending_users")
                        .queryParam("status", "eq." + status)
                        .queryParam("order", "submitted_at.desc")
                        .build())
                .retrieve()
                .bodyToMono(List.class)
                .block();

        if (response != null) {
            return response.stream()
                    .map(this::convertToEntity)
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    public List<PendingUser> findByEmailAndRole(String email, String role) {
        List<Map<String, Object>> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/rest/v1/pending_users")
                        .queryParam("email", "eq." + email)
                        .queryParam("role", "eq." + role)
                        .build())
                .retrieve()
                .bodyToMono(List.class)
                .block();

        if (response != null) {
            return response.stream()
                    .map(this::convertToEntity)
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    private Map<String, Object> convertToMap(PendingUser user) {
        Map<String, Object> map = new java.util.HashMap<>();
        if (user.getId() != null)
            map.put("id", user.getId());
        map.put("email", user.getEmail());
        map.put("password_hash", user.getPasswordHash());
        map.put("role", user.getRole());
        map.put("company_name", user.getCompanyName());
        map.put("company_email", user.getCompanyEmail());
        map.put("company_number", user.getCompanyNumber());
        map.put("form16_url", user.getForm16Url());
        map.put("incorporation_document_url", user.getIncorporationDocumentUrl());
        map.put("financial_document_url", user.getFinancialDocumentUrl());
        map.put("balance_income_statement_url", user.getBalanceIncomeStatementUrl());
        map.put("balance_sheet_url", user.getBalanceSheetUrl());
        map.put("income_statement_url", user.getIncomeStatementUrl());
        map.put("sole_doc1_url", user.getSoleDoc1Url());
        map.put("sole_doc2_url", user.getSoleDoc2Url());
        map.put("sole_doc3_url", user.getSoleDoc3Url());
        map.put("status", user.getStatus());
        map.put("submitted_at", user.getSubmittedAt().toString());
        return map;
    }

    private PendingUser convertToEntity(Map<String, Object> map) {
        return PendingUser.builder()
                .id(UUID.fromString(map.get("id").toString()))
                .email((String) map.get("email"))
                .passwordHash((String) map.get("password_hash"))
                .role((String) map.get("role"))
                .companyName((String) map.get("company_name"))
                .companyEmail((String) map.get("company_email"))
                .companyNumber((String) map.get("company_number"))
                .form16Url((String) map.get("form16_url"))
                .incorporationDocumentUrl((String) map.get("incorporation_document_url"))
                .financialDocumentUrl((String) map.get("financial_document_url"))
                .balanceIncomeStatementUrl((String) map.get("balance_income_statement_url"))
                .balanceSheetUrl((String) map.get("balance_sheet_url"))
                .incomeStatementUrl((String) map.get("income_statement_url"))
                .soleDoc1Url((String) map.get("sole_doc1_url"))
                .soleDoc2Url((String) map.get("sole_doc2_url"))
                .soleDoc3Url((String) map.get("sole_doc3_url"))
                .status((String) map.get("status"))
                .submittedAt(LocalDateTime.parse((String) map.get("submitted_at")))
                .build();
    }
}