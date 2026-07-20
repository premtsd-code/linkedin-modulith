package com.premtsd.linkedin.jobs.internal.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * A durable unit of work. One row per job; the claim path
 * ({@code status = PENDING AND run_at <= now ... FOR UPDATE SKIP LOCKED}) and the
 * reaper ({@code status = RUNNING AND lease_until < now}) both read this table.
 */
@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRecord {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String type;

    /** JSON-encoded immutable payload. */
    @Column(columnDefinition = "text")
    private String payload;

    /** Mutable progress marker, survives across attempts. */
    @Column(columnDefinition = "text")
    private String checkpoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private JobStatus status;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private int maxAttempts = 5;

    @Column(name = "run_at", nullable = false)
    private Instant runAt;

    @Column(name = "lease_until")
    private Instant leaseUntil;

    @Column(name = "worker_id")
    private String workerId;

    /** Non-null only for enqueueOnce jobs; the partial unique index enforces one live job per key. */
    @Column(name = "dedupe_key")
    private String dedupeKey;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;
}
