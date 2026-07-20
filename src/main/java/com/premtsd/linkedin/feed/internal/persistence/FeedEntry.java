package com.premtsd.linkedin.feed.internal.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * One materialized feed row: post {@code postId} (by {@code authorId}) in {@code ownerId}'s
 * feed. The unique (owner_id, post_id) constraint makes fanout idempotent — the overlapping
 * page after a resume re-inserts harmlessly.
 */
@Entity
@Table(name = "feed_entries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "post_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class FeedEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
