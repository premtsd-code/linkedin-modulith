package com.premtsd.linkedin.jobs.internal.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.premtsd.linkedin.jobs.JobScheduler;
import com.premtsd.linkedin.jobs.internal.domain.JobRecord;
import com.premtsd.linkedin.jobs.internal.domain.JobStatus;
import com.premtsd.linkedin.jobs.internal.persistence.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class DefaultJobScheduler implements JobScheduler {

    private final JobRepository repository;
    private final ObjectMapper mapper;

    @Override
    public UUID enqueue(String type, Map<String, Object> payload) {
        return enqueueAt(type, payload, Instant.now());
    }

    @Override
    public UUID enqueueAfter(String type, Map<String, Object> payload, Duration delay) {
        return enqueueAt(type, payload, Instant.now().plus(delay));
    }

    @Override
    @Transactional
    public UUID enqueueAt(String type, Map<String, Object> payload, Instant runAt) {
        return save(type, null, payload, runAt).getId();
    }

    @Override
    @Transactional
    public UUID enqueueOnce(String type, String dedupeKey, Map<String, Object> payload) {
        // Fast path: a live job for this key already exists.
        List<JobRecord> live = repository.findByDedupeKeyAndStatusIn(
                dedupeKey, List.of(JobStatus.PENDING, JobStatus.RUNNING));
        if (!live.isEmpty()) {
            return live.get(0).getId();
        }
        try {
            return save(type, dedupeKey, payload, Instant.now()).getId();
        } catch (DataIntegrityViolationException race) {
            // Lost the race; the partial unique index rejected the duplicate. Return the winner.
            List<JobRecord> now = repository.findByDedupeKeyAndStatusIn(
                    dedupeKey, List.of(JobStatus.PENDING, JobStatus.RUNNING));
            if (!now.isEmpty()) {
                return now.get(0).getId();
            }
            throw race;
        }
    }

    @Override
    @Transactional
    public boolean cancel(UUID jobId) {
        return repository.findById(jobId)
                .filter(j -> j.getStatus() == JobStatus.PENDING)
                .map(j -> {
                    repository.delete(j);
                    return true;
                })
                .orElse(false);
    }

    private JobRecord save(String type, String dedupeKey, Map<String, Object> payload, Instant runAt) {
        JobRecord job = JobRecord.builder()
                .type(type)
                .dedupeKey(dedupeKey)
                .payload(toJson(payload))
                .status(JobStatus.PENDING)
                .runAt(runAt)
                .createdAt(Instant.now())
                .build();
        return repository.save(job);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return mapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Job payload is not JSON-serializable", e);
        }
    }
}
