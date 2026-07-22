package com.premtsd.linkedin.notification.internal;

import java.time.Instant;

/**
 * Storage-neutral view of a notification returned by {@link NotificationStore}. The id is a
 * {@code String} — the relational adapter stringifies its {@code Long} identity — so the
 * module's service and controller stay independent of how the store keys its rows.
 */
record NotificationItem(String id, Long userId, String message, boolean read, Instant createdAt) {
}
