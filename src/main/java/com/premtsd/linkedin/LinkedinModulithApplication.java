package com.premtsd.linkedin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Modular monolith entry point.
 *
 * Each direct sub-package of this package is a Spring Modulith "module":
 *   - user          : authentication, accounts (owns the User aggregate)
 *   - notification  : in-app notifications (a worker, driven by events)
 *   - config        : cross-cutting web/security wiring (not a business module)
 *
 * Modules talk to each other ONLY through published events or exposed APIs.
 * Boundaries are verified by {@code ModularityTests}.
 */
@SpringBootApplication
public class LinkedinModulithApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkedinModulithApplication.class, args);
    }
}
