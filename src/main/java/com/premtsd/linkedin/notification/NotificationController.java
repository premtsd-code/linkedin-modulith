package com.premtsd.linkedin.notification;

import com.premtsd.linkedin.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationView> myNotifications() {
        return notificationService.forUser(SecurityUtils.currentUserId()).stream()
                .map(n -> new NotificationView(n.getId(), n.getMessage(), n.isRead(), n.getCreatedAt()))
                .toList();
    }

    record NotificationView(Long id, String message, boolean read, Instant createdAt) {
    }
}
