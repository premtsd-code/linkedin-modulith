package com.premtsd.linkedin.uploader;

import org.springframework.web.multipart.MultipartFile;

/**
 * Exposed API of the uploader module: store a file, get back a URL.
 *
 * This is the seam that lets the post module call the uploader with a plain
 * in-process method call (replacing the microservices Feign client), and lets us
 * swap the local-disk implementation for a cloud one (Cloudinary/GCS) by profile,
 * with no change to callers.
 */
public interface FileStorage {
    String store(MultipartFile file);
}
