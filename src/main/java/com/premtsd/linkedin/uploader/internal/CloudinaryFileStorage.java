package com.premtsd.linkedin.uploader.internal;

import com.premtsd.linkedin.uploader.FileStorage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Cloud storage via Cloudinary — the microservices uploader-service's real backend.
 * Active only under the 'cloudinary' profile with credentials supplied:
 *   --spring.profiles.active=cloudinary
 *   CLOUDINARY_CLOUD_NAME=... CLOUDINARY_API_KEY=... CLOUDINARY_API_SECRET=...
 */
@Service
@Profile("cloudinary")
@Slf4j
class CloudinaryFileStorage implements FileStorage {

    private final Cloudinary cloudinary;

    CloudinaryFileStorage(@Value("${cloudinary.cloud-name}") String cloudName,
                          @Value("${cloudinary.api-key}") String apiKey,
                          @Value("${cloudinary.api-secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided");
        }
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("resource_type", "auto"));
            String url = String.valueOf(result.get("secure_url"));
            log.info("Uploaded to Cloudinary: {}", url);
            return url;
        } catch (IOException e) {
            throw new UncheckedIOException("Cloudinary upload failed", e);
        }
    }
}
