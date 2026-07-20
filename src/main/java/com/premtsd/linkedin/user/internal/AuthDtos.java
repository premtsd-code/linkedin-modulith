package com.premtsd.linkedin.user.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request/response payloads for the auth endpoints. Package-private records =
 * module-internal; nothing outside the user module can bind to them.
 */
class AuthDtos {

    record SignupRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            @NotBlank String password) {
    }

    record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password) {
    }

    record TokenResponse(String token) {
    }
}
