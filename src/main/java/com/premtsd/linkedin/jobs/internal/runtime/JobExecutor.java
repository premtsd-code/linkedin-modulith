package com.premtsd.linkedin.jobs.internal.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.premtsd.linkedin.jobs.JobRunner;
import com.premtsd.linkedin.jobs.internal.domain.JobRecord;
import com.premtsd.linkedin.jobs.internal.domain.JobStatus;
import com.premtsd.linkedin.jobs.internal.persistence.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Runs the claim → execute → outcome cycle. Each of claim, complete, fail and reap is
 * its own short transaction (via {@link TransactionTemplate}); the runner itself
 * executes with NO transaction held, so a slow job never pins a connection.
 *
 * Worker role only — the executor and poller exist only where jobs actually run
 * (Postgres, which is required for {@code FOR UPDATE SKIP LOCKED}).
 */
@Component
@ConditionalOnProperty(name = "app.role", havingValue = "worker")
@Slf4j
class JobExecutor {

    private final JobRepository repo;
    private final ObjectMapper mapper;
    private final JobProperties props;
    private final TransactionTemplate tx;
    private final Map<String, JobRunner> runners;

    JobExecutor(JobRepository repo, ObjectMapper mapper, JobProperties props,
                PlatformTransactionManager txManager, List<JobRunner> runnerBeans) {
        this.repo = repo;
        this.mapper = mapper;
        this.props = props;
        this.tx = new TransactionTemplate(txManager);
        this.runners = runnerBeans.stream().collect(toMap(JobRunner::type, identity()));
        log.info("JobExecutor started with runners: {}", runners.keySet());
    }

    /** Claim up to {@code limit} runnable jobs: select+lock then mark RUNNING, one transaction. */
    List<UUID> claim(String workerId, int limit) {
        return tx.execute(status -> {
            Instant now = Instant.now();
            List<UUID> ids = repo.selectClaimable(now, limit);
            if (ids.isEmpty()) {
                return List.of();
            }
            repo.markRunning(ids, workerId, now.plus(props.getLeaseDuration()), now);
            return ids;
        });
    }

    /** Load, run (untransacted), then persist the outcome. Never throws to the caller. */
    void run(UUID id) {
        JobRecord job = tx.execute(s -> repo.findById(id).orElse(null));
        if (job == null) {
            return;
        }
        JobRunner runner = runners.get(job.getType());
        int maxAttempts = runner != null ? runner.maxAttempts() : job.getMaxAttempts();
        if (runner == null) {
            log.error("No JobRunner registered for type '{}' (job {})", job.getType(), id);
            fail(job.getId(), maxAttempts);
            return;
        }
        DefaultJobContext ctx = new DefaultJobContext(job, mapper, repo, props.getLeaseDuration());
        try {
            runner.run(ctx);
            complete(job.getId(), ctx);
        } catch (Exception e) {
            log.warn("Job {} ({}) attempt {} failed: {}", id, job.getType(), job.getAttempts(), e.toString());
            fail(job.getId(), maxAttempts);
        }
    }

    private void complete(UUID id, DefaultJobContext ctx) {
        tx.executeWithoutResult(s -> {
            JobRecord j = repo.findById(id).orElseThrow();
            Instant now = Instant.now();
            if (ctx.rescheduled) {
                j.setCheckpoint(ctx.nextCheckpoint);
                j.setStatus(JobStatus.PENDING);
                j.setAttempts(Math.max(0, j.getAttempts() - 1)); // progress is not a failed attempt
                j.setRunAt(now);
                j.setLeaseUntil(null);
                j.setWorkerId(null);
            } else {
                j.setStatus(JobStatus.COMPLETED);
                j.setLeaseUntil(null);
                j.setWorkerId(null);
            }
            j.setUpdatedAt(now);
            repo.save(j);
        });
    }

    private void fail(UUID id, int maxAttempts) {
        tx.executeWithoutResult(s -> {
            JobRecord j = repo.findById(id).orElseThrow();
            Instant now = Instant.now();
            if (j.getAttempts() >= maxAttempts) {
                j.setStatus(JobStatus.FAILED);
                j.setLeaseUntil(null);
                j.setWorkerId(null);
            } else {
                j.setStatus(JobStatus.PENDING);
                j.setWorkerId(null);
                j.setLeaseUntil(null);
                j.setRunAt(now.plus(backoff(j.getAttempts())));
            }
            j.setUpdatedAt(now);
            repo.save(j);
        });
    }

    /** Return jobs whose worker died (expired lease) to PENDING. */
    int reap() {
        Integer n = tx.execute(s -> repo.reap(Instant.now()));
        return n == null ? 0 : n;
    }

    private Duration backoff(int attempts) {
        long base = props.getRetryBaseDelay().toMillis();
        long millis = base << Math.min(attempts, 16); // exponential, exponent capped to avoid overflow
        return Duration.ofMillis(Math.min(millis, props.getRetryMaxDelay().toMillis()));
    }
}
