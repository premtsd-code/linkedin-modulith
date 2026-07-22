package com.premtsd.linkedin.platform.persistence;

import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * The sticky window, per request. Before handling: if the authenticated user wrote recently,
 * pin this whole request's read-only transactions to the primary (via {@link RoutingHints}) so
 * they can't land on a lagging replica. After handling: if this was a successful mutation,
 * mark the user as having just written, starting their window.
 *
 * <p>Generic — no per-controller changes. A mutating HTTP method (POST/PUT/PATCH/DELETE) that
 * returns < 400 counts as a write. Runs as an MVC interceptor, so it executes after the JWT
 * filter has populated the security context and on the same thread as the handler (which is
 * why the {@link RoutingHints} thread-local reaches the routing datasource).
 */
@Component
@Profile("feeddb")
class ReadYourWritesInterceptor implements HandlerInterceptor {

    private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final RecentWrites recentWrites;

    ReadYourWritesInterceptor(RecentWrites recentWrites) {
        this.recentWrites = recentWrites;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = currentUserOrNull();
        if (userId != null && recentWrites.wroteRecently(userId)) {
            RoutingHints.pinToPrimary();
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        try {
            Long userId = currentUserOrNull();
            if (userId != null && MUTATING.contains(request.getMethod()) && response.getStatus() < 400) {
                recentWrites.markWritten(userId);
            }
        } finally {
            RoutingHints.clear();   // never leak the pin onto the next request on this pooled thread
        }
    }

    private static Long currentUserOrNull() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getPrincipal() instanceof Long userId) ? userId : null;
    }
}
