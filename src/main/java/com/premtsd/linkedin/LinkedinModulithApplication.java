package com.premtsd.linkedin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Modular monolith entry point.
 *
 * Each direct sub-package of this package is a Spring Modulith module:
 *   - user, post, connections, notification, uploader : business modules
 *   - feed                                            : event-driven fan-out (needs workers)
 *   - jobs                                            : generic durable job runtime
 *   - platform                                        : cross-cutting technical wiring (OPEN)
 *   - shared                                          : shared value types + SecurityUtils (OPEN)
 *
 * Modules talk to each other ONLY through published events or exposed APIs; the
 * allowedDependencies on each module are enforced by {@code ModularityTests}.
 *
 * @EnableScheduling powers the worker-role job poller/reaper and the feed cron.
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class LinkedinModulithApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkedinModulithApplication.class, args);
    }
}
