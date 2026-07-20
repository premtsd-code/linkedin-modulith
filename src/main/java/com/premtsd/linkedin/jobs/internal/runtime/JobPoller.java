package com.premtsd.linkedin.jobs.internal.runtime;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Drives the executor on a fixed poll. Worker role only; the web role enqueues but has
 * no poller bean. Each claimed job runs on a virtual thread; a semaphore bounds
 * in-flight work so the poller never claims more than it can immediately run — otherwise
 * leases would tick down on jobs sitting in a queue.
 */
@Component
@ConditionalOnProperty(name = "app.role", havingValue = "worker")
@RequiredArgsConstructor
@Slf4j
class JobPoller {

    private final JobExecutor executor;
    private final JobProperties props;

    private final String workerId = "worker-" + UUID.randomUUID();
    private ExecutorService pool;
    private Semaphore slots;

    @PostConstruct
    void start() {
        this.pool = Executors.newVirtualThreadPerTaskExecutor();
        this.slots = new Semaphore(props.getConcurrency());
        log.info("JobPoller {} started (concurrency={}, batch={})",
                workerId, props.getConcurrency(), props.getBatchSize());
    }

    @Scheduled(fixedDelayString = "${jobs.poll-interval:1s}")
    void poll() {
        int free = slots.availablePermits();
        if (free == 0) {
            return; // fully loaded; don't claim jobs we can't start (their leases would tick)
        }
        int limit = Math.min(props.getBatchSize(), free);
        List<UUID> ids = executor.claim(workerId, limit);
        for (UUID id : ids) {
            slots.acquireUninterruptibly();
            pool.submit(() -> {
                try {
                    executor.run(id);
                } finally {
                    slots.release();
                }
            });
        }
    }

    @Scheduled(fixedDelayString = "${jobs.reaper-interval:30s}")
    void reap() {
        int n = executor.reap();
        if (n > 0) {
            log.info("Reaper returned {} stale job(s) to PENDING", n);
        }
    }

    @PreDestroy
    void stop() {
        if (pool != null) {
            pool.shutdown();
        }
    }
}
