package com.premtsd.linkedin.jobs.internal.persistence;

import com.premtsd.linkedin.jobs.internal.domain.JobRecord;
import com.premtsd.linkedin.jobs.internal.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<JobRecord, UUID> {

    /**
     * Claim step 1: pick ids of runnable jobs and lock their rows, skipping rows other
     * workers already hold. Postgres-only ({@code SKIP LOCKED}). MUST run in the same
     * transaction as {@link #markRunning} or the locks release before the claim is written.
     */
    @Query(value = """
            SELECT id FROM jobs
            WHERE status = 'PENDING' AND run_at <= :now
            ORDER BY run_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<UUID> selectClaimable(@Param("now") Instant now, @Param("limit") int limit);

    /** Claim step 2: mark the locked rows RUNNING and count this attempt. */
    @Modifying
    @Query("""
            update JobRecord j
               set j.status = com.premtsd.linkedin.jobs.internal.domain.JobStatus.RUNNING,
                   j.workerId = :workerId,
                   j.leaseUntil = :leaseUntil,
                   j.attempts = j.attempts + 1,
                   j.updatedAt = :now
             where j.id in :ids and j.status = com.premtsd.linkedin.jobs.internal.domain.JobStatus.PENDING
            """)
    int markRunning(@Param("ids") List<UUID> ids,
                    @Param("workerId") String workerId,
                    @Param("leaseUntil") Instant leaseUntil,
                    @Param("now") Instant now);

    /** Reaper: return jobs whose worker died (lease expired) to PENDING. */
    @Modifying
    @Query("""
            update JobRecord j
               set j.status = com.premtsd.linkedin.jobs.internal.domain.JobStatus.PENDING,
                   j.workerId = null,
                   j.leaseUntil = null,
                   j.runAt = :now,
                   j.updatedAt = :now
             where j.status = com.premtsd.linkedin.jobs.internal.domain.JobStatus.RUNNING
               and j.leaseUntil < :now
            """)
    int reap(@Param("now") Instant now);

    /** Extend a running job's lease (heartbeat). */
    @Modifying
    @Query("update JobRecord j set j.leaseUntil = :leaseUntil where j.id = :id")
    int extendLease(@Param("id") UUID id, @Param("leaseUntil") Instant leaseUntil);

    List<JobRecord> findByDedupeKeyAndStatusIn(String dedupeKey, List<JobStatus> statuses);
}
