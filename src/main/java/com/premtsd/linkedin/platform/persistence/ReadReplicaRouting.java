package com.premtsd.linkedin.platform.persistence;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Read/write splitting for one logical database: a {@link AbstractRoutingDataSource} that
 * sends read-only transactions to a replica endpoint and everything else to the primary.
 *
 * <p>The routing key is the current transaction's {@code readOnly} flag, so a method marked
 * {@code @Transactional(readOnly = true)} — including Spring Data's query methods, which are
 * read-only by default — lands on the replica; writes, DDL, and non-transactional access
 * (key resolves to {@code write}) land on the primary. No per-call annotation is needed
 * beyond the {@code readOnly} flag the read paths already carry.
 *
 * <p>A read wrapped in {@link RoutingHints#fromPrimary} routes to the primary even while
 * read-only — the read-your-writes escape hatch for reads that must not see a lagging replica.
 *
 * <p>The result MUST be wrapped in {@link LazyConnectionDataSourceProxy}: without it the
 * persistence unit grabs a physical connection when the transaction opens — before Spring
 * has published the {@code readOnly} flag — and every read would route to the primary. The
 * lazy proxy defers connection acquisition to the first statement, by which point the flag
 * is set and {@link #routing} reads the correct key.
 *
 * <p>The {@code read} target is a single endpoint on purpose: point it at a cloud reader
 * endpoint (e.g. Aurora) or an HAProxy/PgPool in front of N replicas, and fan-out across
 * replicas stays an infrastructure concern — the app sees one read URL.
 */
final class ReadReplicaRouting {

    private static final String WRITE = "write";
    private static final String READ = "read";

    private ReadReplicaRouting() {
    }

    static DataSource of(DataSource write, DataSource read) {
        AbstractRoutingDataSource router = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
                return (readOnly && !RoutingHints.isPrimaryForced()) ? READ : WRITE;
            }
        };
        router.setTargetDataSources(Map.of(WRITE, write, READ, read));
        router.setDefaultTargetDataSource(write);
        router.afterPropertiesSet();
        return new LazyConnectionDataSourceProxy(router);
    }
}
