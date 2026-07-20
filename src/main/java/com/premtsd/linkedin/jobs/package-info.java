/**
 * Jobs module — a generic, domain-free durable job runtime for work that cannot be
 * a one-shot event listener (chunked fanout, checkpointed long jobs, scheduled work
 * that needs retries). Domain modules depend on {@code jobs}; {@code jobs} depends on
 * nothing domain-specific. Claiming is arbitrated by the database
 * ({@code FOR UPDATE SKIP LOCKED}) — no registry, no leader election.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Jobs",
        allowedDependencies = {"shared"})
package com.premtsd.linkedin.jobs;
