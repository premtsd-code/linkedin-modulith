/**
 * Platform module — cross-cutting technical wiring that is not a business module:
 * security (JWT filter + Spring Security chain), web (global error handling) and
 * messaging (Kafka topic provisioning + the inbound event relay). Declared OPEN so
 * it may depend on any module's exposed API and is not itself dependency-checked.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Platform",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.premtsd.linkedin.platform;
