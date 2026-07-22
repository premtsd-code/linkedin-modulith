package com.premtsd.linkedin.notification.internal;

import java.util.List;

/**
 * Port for notification persistence, implemented by {@link JpaNotificationStore} — relational
 * rows in Postgres/H2 (a dedicated Postgres under the 'feeddb' profile, the primary otherwise).
 * {@link NotificationService} depends only on this port, keeping persistence behind the module
 * boundary.
 */
interface NotificationStore {

    void save(Long userId, String message);

    List<NotificationItem> findByUserNewestFirst(Long userId);

    long countUnread(Long userId);

    /** Distinct user ids ({@code > afterUserId}, ascending, capped) that currently have unread notifications. */
    List<Long> unreadOwnerIdsAfter(long afterUserId, int limit);
}
