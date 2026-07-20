package com.premtsd.linkedin.jobs;

import com.premtsd.linkedin.jobs.internal.domain.JobRecord;
import com.premtsd.linkedin.jobs.internal.domain.JobStatus;
import com.premtsd.linkedin.jobs.internal.persistence.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Real integration of the durable job runtime against Postgres — the only engine with
 * {@code FOR UPDATE SKIP LOCKED}. Covers execution, enqueueOnce idempotence,
 * retry-with-backoff and checkpoint resume.
 *
 * <p>Requires Docker; {@code disabledWithoutDocker = true} SKIPS the whole class when no
 * Docker daemon is present, so the H2 suite still runs green in environments without it.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = {com.premtsd.linkedin.LinkedinModulithApplication.class, JobRuntimeTests.TestRunners.class},
        properties = {
                "app.role=worker",
                "app.messaging.inbound-relay=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
                "spring.jpa.hibernate.ddl-auto=update",
                "spring.flyway.enabled=false",
                "spring.modulith.events.externalization.enabled=false",
                "jobs.poll-interval=200ms",
                "jobs.reaper-interval=500ms",
                "jobs.lease-duration=5s",
                "jobs.retry-base-delay=100ms",
                "jobs.retry-max-delay=1s"
        })
class JobRuntimeTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JobScheduler scheduler;
    @Autowired JobRepository jobs;
    @Autowired TestRunners runners;

    @Test
    void runs_a_job_to_completion() {
        UUID id = scheduler.enqueue("test.echo", Map.of("value", 42));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(jobs.findById(id)).get()
                        .extracting(JobRecord::getStatus).isEqualTo(JobStatus.COMPLETED));
        assertThat(runners.echoCount.get()).isEqualTo(1);
        assertThat(runners.lastEchoValue.get()).isEqualTo(42);
    }

    @Test
    void enqueueOnce_is_idempotent() {
        UUID a = scheduler.enqueueOnce("test.noop", "dupe-key", Map.of());
        UUID b = scheduler.enqueueOnce("test.noop", "dupe-key", Map.of());

        assertThat(a).isEqualTo(b);
        assertThat(jobs.findByDedupeKeyAndStatusIn("dupe-key",
                java.util.List.of(JobStatus.PENDING, JobStatus.RUNNING)))
                .hasSize(1);
    }

    @Test
    void retries_with_backoff_then_succeeds() {
        UUID id = scheduler.enqueue("test.flaky", Map.of());

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(jobs.findById(id)).get()
                        .extracting(JobRecord::getStatus).isEqualTo(JobStatus.COMPLETED));
        assertThat(runners.flakyAttempts.get()).isEqualTo(3); // failed twice, third succeeds
    }

    @Test
    void resumes_from_checkpoint_across_reschedules() {
        UUID id = scheduler.enqueue("test.resume", Map.of());

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(jobs.findById(id)).get()
                        .extracting(JobRecord::getStatus).isEqualTo(JobStatus.COMPLETED));
        // cursor advances 0 -> 1 -> 2 -> 3 (4 invocations), the last returning no more work
        assertThat(runners.resumeInvocations.get()).isEqualTo(4);
    }

    @TestConfiguration
    static class TestRunners {
        final AtomicInteger echoCount = new AtomicInteger();
        final AtomicInteger lastEchoValue = new AtomicInteger();
        final AtomicInteger flakyAttempts = new AtomicInteger();
        final AtomicInteger resumeInvocations = new AtomicInteger();

        @Bean
        JobRunner echoRunner() {
            return new JobRunner() {
                public String type() { return "test.echo"; }
                public void run(JobContext ctx) {
                    lastEchoValue.set(ctx.arg("value", Integer.class));
                    echoCount.incrementAndGet();
                }
            };
        }

        @Bean
        JobRunner noopRunner() {
            return new JobRunner() {
                public String type() { return "test.noop"; }
                public void run(JobContext ctx) { /* no-op */ }
            };
        }

        @Bean
        JobRunner flakyRunner() {
            return new JobRunner() {
                public String type() { return "test.flaky"; }
                public void run(JobContext ctx) {
                    if (flakyAttempts.incrementAndGet() < 3) {
                        throw new IllegalStateException("transient failure");
                    }
                }
            };
        }

        @Bean
        JobRunner resumeRunner() {
            return new JobRunner() {
                public String type() { return "test.resume"; }
                public void run(JobContext ctx) {
                    resumeInvocations.incrementAndGet();
                    long cursor = ctx.checkpointAsLong(0L);
                    if (cursor < 3) {
                        ctx.reschedule(cursor + 1);
                    }
                }
            };
        }
    }
}
