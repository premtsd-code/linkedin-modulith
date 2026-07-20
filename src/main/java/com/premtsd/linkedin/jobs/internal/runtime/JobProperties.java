package com.premtsd.linkedin.jobs.internal.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Runtime tuning for the poller/executor. Bound from the {@code jobs.*} config.
 * See application.yml for the pitfalls (lease vs slowest slice, concurrency vs pool).
 */
@ConfigurationProperties(prefix = "jobs")
public class JobProperties {

    private Duration pollInterval = Duration.ofSeconds(1);
    private Duration reaperInterval = Duration.ofSeconds(30);
    private Duration leaseDuration = Duration.ofSeconds(60);
    private int batchSize = 20;
    private int concurrency = 8;
    private Duration retryBaseDelay = Duration.ofSeconds(5);
    private Duration retryMaxDelay = Duration.ofMinutes(15);

    public Duration getPollInterval() { return pollInterval; }
    public void setPollInterval(Duration v) { this.pollInterval = v; }

    public Duration getReaperInterval() { return reaperInterval; }
    public void setReaperInterval(Duration v) { this.reaperInterval = v; }

    public Duration getLeaseDuration() { return leaseDuration; }
    public void setLeaseDuration(Duration v) { this.leaseDuration = v; }

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int v) { this.batchSize = v; }

    public int getConcurrency() { return concurrency; }
    public void setConcurrency(int v) { this.concurrency = v; }

    public Duration getRetryBaseDelay() { return retryBaseDelay; }
    public void setRetryBaseDelay(Duration v) { this.retryBaseDelay = v; }

    public Duration getRetryMaxDelay() { return retryMaxDelay; }
    public void setRetryMaxDelay(Duration v) { this.retryMaxDelay = v; }
}
