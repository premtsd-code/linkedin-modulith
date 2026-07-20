package com.premtsd.linkedin;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * The core value of Spring Modulith: module boundaries are ENFORCED, not just
 * hoped for. This test fails the build if any module reaches into another
 * module's internals instead of going through its exposed API/events.
 */
class ModularityTests {

    static final ApplicationModules modules = ApplicationModules.of(LinkedinModulithApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        modules.verify();
    }

    @Test
    void writesDocumentation() {
        // Generates C4/PlantUML component diagrams + module docs under
        // target/spring-modulith-docs -- a free architecture diagram for the README.
        new Documenter(modules)
                .writeDocumentation()
                .writeIndividualModulesAsPlantUml();
    }
}
