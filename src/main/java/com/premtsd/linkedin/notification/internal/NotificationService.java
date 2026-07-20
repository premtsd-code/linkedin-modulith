package com.premtsd.linkedin.notification.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    @Transactional
    public void create(Long userId, String message) {
        repository.save(Notification.builder().userId(userId).message(message).build());
    }

    @Transactional(readOnly = true)
    List<Notification> forUser(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Keyset page (ids &gt; afterUserId) of users who currently have unread notifications. */
    @Transactional(readOnly = true)
    public List<Long> usersWithUnreadAfter(long afterUserId, int limit) {
        return repository.findUnreadOwnerIdsAfter(afterUserId, PageRequest.of(0, limit));
    }

    /** Roll a user's unread notifications into one digest notification. No-op if none unread. */
    @Transactional
    public void sendDigest(Long userId) {
        long unread = repository.countByUserIdAndReadFalse(userId);
        if (unread > 0) {
            create(userId, "Digest: you have " + unread + " unread notification" + (unread == 1 ? "" : "s") + ".");
        }
    }
}
