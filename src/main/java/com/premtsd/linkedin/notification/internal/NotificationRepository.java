package com.premtsd.linkedin.notification.internal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);

    /** Keyset page of user ids that have at least one unread notification, id > afterUserId. */
    @Query("""
            select distinct n.userId from Notification n
            where n.read = false and n.userId > :after
            order by n.userId
            """)
    List<Long> findUnreadOwnerIdsAfter(@Param("after") long after, Pageable pageable);
}
