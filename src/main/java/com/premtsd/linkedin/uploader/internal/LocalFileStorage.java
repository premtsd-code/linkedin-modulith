package com.premtsd.linkedin.uploader.internal;

import com.premtsd.linkedin.uploader.FileStorage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Default storage: writes to local disk. Active unless the 'cloudinary' profile is on.
 */
@Service
@Profile("!cloudinary")
@Slf4j
class LocalFileStorage implements FileStorage {

    private final Path dir;
    private final String baseUrl;

    LocalFileStorage(@Value("${uploader.dir:target/uploads}") String dir,
                     @Value("${uploader.base-url:http://localhost:8081/files}") String baseUrl) {
        this.dir = Path.of(dir);
        this.baseUrl = baseUrl;
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided");
        }
        try {
            Files.createDirectories(dir);
            String name = UUID.randomUUID() + "-" + file.getOriginalFilename();
            file.transferTo(dir.resolve(name).toAbsolutePath());
            log.info("Stored upload locally: {}", name);
            return baseUrl + "/" + name;
        } catch (IOException e) {
            throw new UncheckedIOException("Upload failed", e);
        }
    }
}
