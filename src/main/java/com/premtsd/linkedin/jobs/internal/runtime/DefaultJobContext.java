package com.premtsd.linkedin.jobs.internal.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.premtsd.linkedin.jobs.JobContext;
import com.premtsd.linkedin.jobs.internal.domain.JobRecord;
import com.premtsd.linkedin.jobs.internal.persistence.JobRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Per-invocation {@link JobContext}. Package-private fields {@link #rescheduled} and
 * {@link #nextCheckpoint} are read by {@link JobExecutor} when persisting the outcome.
 */
class DefaultJobContext implements JobContext {

    private final JobRecord job;
    private final ObjectMapper mapper;
    private final JobRepository repository;
    private final Duration leaseDuration;
    private final Map<String, Object> payload;

    boolean rescheduled = false;
    String nextCheckpoint = null;

    DefaultJobContext(JobRecord job, ObjectMapper mapper, JobRepository repository, Duration leaseDuration) {
        this.job = job;
        this.mapper = mapper;
        this.repository = repository;
        this.leaseDuration = leaseDuration;
        this.payload = parse(job.getPayload(), mapper);
    }

    private static Map<String, Object> parse(String json, ObjectMapper mapper) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Corrupt job payload", e);
        }
    }

    @Override
    public UUID jobId() {
        return job.getId();
    }

    @Override
    public <T> T arg(String key, Class<T> type) {
        return mapper.convertValue(payload.get(key), type);
    }

    @Override
    public long checkpointAsLong(long defaultValue) {
        String c = job.getCheckpoint();
        return (c == null || c.isBlank()) ? defaultValue : Long.parseLong(c.trim());
    }

    @Override
    public String checkpoint() {
        return job.getCheckpoint();
    }

    @Override
    public void reschedule(Object next) {
        this.rescheduled = true;
        this.nextCheckpoint = String.valueOf(next);
    }

    @Override
    public void heartbeat() {
        repository.extendLease(job.getId(), Instant.now().plus(leaseDuration));
    }
}
