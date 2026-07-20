package com.premtsd.linkedin.connections.internal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "connection_requests",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sender_id", "receiver_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ConnectionRequest {

    enum Status { PENDING, ACCEPTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
}
