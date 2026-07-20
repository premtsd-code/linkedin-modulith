package com.premtsd.linkedin;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * shared is the one module every other module depends on, so it must stay a leaf of pure
 * value types + tiny utilities — no beans, no persistence, no web. That rule is the single
 * point in the design that {@code ModularityTests.verify()} does NOT enforce (an OPEN module
 * with only public types trivially passes boundary checks), and shared modules rot precisely
 * because "just don't put a @Service here" relies on willpower. This test is the guard: it
 * fails the build the moment someone adds a Spring stereotype, a @Configuration, an @Entity,
 * a repository, or a controller to shared.
 *
 * SecurityUtils' use of spring-security-core (SecurityContextHolder) is intentional and left
 * allowed; everything framework-heavy is denied.
 */
class SharedModulePurityTests {

    static final JavaClasses sharedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.premtsd.linkedin.shared");

    @Test
    void sharedHasNoFrameworkDependencies() {
        noClasses().that().resideInAPackage("com.premtsd.linkedin.shared..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..",                 // @Entity / JPA
                        "org.springframework.stereotype..",      // @Component/@Service/@Repository/@Controller
                        "org.springframework.context..",         // @Configuration/@Bean/ApplicationContext
                        "org.springframework.web..",             // controllers / web
                        "org.springframework.data..",            // repositories
                        "org.springframework.boot..")            // autoconfiguration
                .because("shared must stay a pure leaf of value types + utilities — no beans, "
                        + "no persistence, no web. Put framework code in the owning module or platform.")
                .check(sharedClasses);
    }
}
