package com.premtsd.linkedin.connections.internal;

import com.premtsd.linkedin.connections.internal.ConnectionRequest.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, Long> {

    Optional<ConnectionRequest> findBySenderIdAndReceiverId(Long senderId, Long receiverId);

    boolean existsBySenderIdAndReceiverId(Long senderId, Long receiverId);

    // first-degree = every ACCEPTED edge touching the user, from either side
    @Query("""
            select case when c.senderId = :userId then c.receiverId else c.senderId end
            from ConnectionRequest c
            where c.status = :status and (c.senderId = :userId or c.receiverId = :userId)
            """)
    List<Long> findConnectedUserIds(@Param("userId") Long userId, @Param("status") Status status);
}
