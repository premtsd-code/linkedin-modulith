package com.premtsd.linkedin.notification.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Notification rules. Depends only on the {@link NotificationStore} port, so the same logic
 * runs whether notifications live in the primary database or the dedicated one (the 'feeddb'
 * profile). Each operation is a single store call, so no multi-statement transaction is needed.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationStore store;

    public void create(Long userId, String message) {
        store.save(userId, message);
    }

    List<NotificationItem> forUser(Long userId) {
        return store.findByUserNewestFirst(userId);
    }

    /** Keyset page (ids &gt; afterUserId) of users who currently have unread notifications. */
    public List<Long> usersWithUnreadAfter(long afterUserId, int limit) {
        return store.unreadOwnerIdsAfter(afterUserId, limit);
    }

    /** Roll a user's unread notifications into one digest notification. No-op if none unread. */
    public void sendDigest(Long userId) {
        long unread = store.countUnread(userId);
        if (unread > 0) {
            create(userId, "Digest: you have " + unread + " unread notification" + (unread == 1 ? "" : "s") + ".");
        }
    }
}
