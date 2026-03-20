package com.authservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    private final String BUCKET = "auth-documents";

    public String uploadFile(MultipartFile file, String folderPath) {

        try {

            String fileName = folderPath + "/" +
                    UUID.randomUUID() + "_" + file.getOriginalFilename();

            String url = supabaseUrl +
                    "/storage/v1/object/" +
                    BUCKET + "/" + fileName;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);

            new RestTemplate().exchange(
                    url, HttpMethod.POST, entity, String.class);

            return supabaseUrl +
                    "/storage/v1/object/public/" +
                    BUCKET + "/" + fileName;

        } catch (Exception e) {
            throw new RuntimeException("File upload failed");
        }
    }
}
