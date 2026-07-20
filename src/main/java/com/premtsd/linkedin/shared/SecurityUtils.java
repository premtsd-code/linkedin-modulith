package com.premtsd.linkedin.shared;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * In-process replacement for the microservices' gateway -> {@code X-User-Id} header trick.
 * Lives in the {@code shared} module so every business module can depend on it
 * without depending on the application shell (which would create a cycle).
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
