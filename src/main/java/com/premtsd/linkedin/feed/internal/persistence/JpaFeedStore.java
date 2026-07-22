package com.premtsd.linkedin.feed.internal.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Relational adapter for {@link FeedStore}. Delegates to the native {@code unnest}/{@code ON
 * CONFLICT} batch insert, which keeps fanout to one round-trip per page on the Postgres tier.
 *
 * <p>Transactions name {@code feedTransactionManager}: under the 'feeddb' profile that is
 * the dedicated feed database (DB B); otherwise it is aliased to the primary manager, so
 * the single-database run is unchanged. See {@code platform.persistence}.
 */
@Component
@RequiredArgsConstructor
class JpaFeedStore implements FeedStore {

    private final FeedEntryRepository repository;

    @Override
    @Transactional("feedTransactionManager")
    public void insertBatch(List<Long> ownerIds, long postId, long authorId) {
        repository.insertBatch(ownerIds.toArray(Long[]::new), postId, authorId);
    }

    @Override
    @Transactional("feedTransactionManager")
    public int deleteOlderThan(Instant cutoff, int limit) {
        return repository.deleteOlderThan(cutoff, limit);
    }
}
