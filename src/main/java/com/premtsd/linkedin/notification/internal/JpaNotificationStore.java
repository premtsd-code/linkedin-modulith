package com.premtsd.linkedin.notification.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Relational adapter for {@link NotificationStore}. Transactions name {@code feedTransactionManager}
 * — the dedicated feed database under the 'feeddb' profile, aliased to the primary manager
 * otherwise. See {@code platform.persistence}.
 */
@Component
@RequiredArgsConstructor
class JpaNotificationStore implements NotificationStore {

    private final NotificationRepository repository;

    @Override
    @Transactional("feedTransactionManager")
    public void save(Long userId, String message) {
        repository.save(Notification.builder().userId(userId).message(message).build());
    }

    @Override
    @Transactional(transactionManager = "feedTransactionManager", readOnly = true)
    public List<NotificationItem> findByUserNewestFirst(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> new NotificationItem(
                        String.valueOf(n.getId()), n.getUserId(), n.getMessage(), n.isRead(), n.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional(transactionManager = "feedTransactionManager", readOnly = true)
    public long countUnread(Long userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional(transactionManager = "feedTransactionManager", readOnly = true)
    public List<Long> unreadOwnerIdsAfter(long afterUserId, int limit) {
        return repository.findUnreadOwnerIdsAfter(afterUserId, PageRequest.of(0, limit));
    }
}
