package com.premtsd.linkedin;

import com.premtsd.linkedin.jobs.JobRunner;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The core value of Spring Modulith: module boundaries are ENFORCED, not just
 * hoped for. This fails the build if any module reaches past another module's
 * exposed API/events (allowedDependencies) or into its internals.
 */
class ModularityTests {

    static final ApplicationModules modules = ApplicationModules.of(LinkedinModulithApplication.class);

    static final JavaClasses appClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.premtsd.linkedin");

    @Test
    void verifiesModuleBoundaries() {
        modules.verify();
    }

    /** Jobs are not a module of their own — each module owns its runners, kept internal. */
    @Test
    void jobRunnersStayInternal() {
        classes().that().implement(JobRunner.class)
                .should().resideInAPackage("..internal.worker..")
                .check(appClasses);
    }

    /** Repositories are never exposed — persistence stays behind each module's boundary. */
    @Test
    void repositoriesStayInternal() {
        classes().that().areInterfaces().and().areAssignableTo(JpaRepository.class)
                .should().resideInAPackage("..internal..")
                .check(appClasses);
    }

    /**
     * shared stays thin: value types + SecurityUtils only, no entities, controllers,
     * repositories or Spring stereotypes. (SecurityUtils' use of spring-security-core is
     * intentional and allowed.)
     */
    @Test
    void sharedStaysThin() {
        noClasses().that().resideInAPackage("..shared..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..",
                        "org.springframework.web..",
                        "org.springframework.data..",
                        "org.springframework.stereotype..")
                .check(appClasses);
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
