package com.premtsd.linkedin;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * In-process replacement for the microservices' gateway -> {@code X-User-Id} header trick.
 * The authenticated user id now lives in the SecurityContext, so there is no
 * spoofable header to trust.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return userId;
    }
}
