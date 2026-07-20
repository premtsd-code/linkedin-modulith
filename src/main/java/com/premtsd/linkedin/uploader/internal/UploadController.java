package com.premtsd.linkedin.uploader.internal;

import com.premtsd.linkedin.uploader.FileStorage;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
class UploadController {

    private final FileStorage fileStorage;

    @PostMapping
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        return Map.of("url", fileStorage.store(file));
    }
}
