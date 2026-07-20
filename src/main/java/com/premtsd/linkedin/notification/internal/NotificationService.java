package com.premtsd.linkedin.notification.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
class NotificationService {

    private final NotificationRepository repository;

    @Transactional
    void create(Long userId, String message) {
        repository.save(Notification.builder().userId(userId).message(message).build());
    }

    @Transactional(readOnly = true)
    List<Notification> forUser(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
