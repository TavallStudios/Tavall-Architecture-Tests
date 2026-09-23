package org.tavall.architecture.di;

import org.junit.jupiter.api.Test;
import org.tavall.architecture.core.ArchitectureContext;
import org.tavall.architecture.core.ArchitectureViolation;
import org.tavall.dependency.IDependencyInjectableConcrete;
import org.tavall.dependency.IDependencyInjectableInterface;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DependencyInjectionRuleTest {
    @Test
    void aggregatesInheritedRetiredMarkersIntoOneClassViolation() {
        String previousRoots = System.getProperty("tavall.architecture.classRoots");
        System.setProperty(
                "tavall.architecture.classRoots",
                Path.of("build/classes/java/test").toAbsolutePath().toString()
        );
        try {
            List<ArchitectureViolation> violations = new DependencyInjectionRule()
                    .validate(ArchitectureContext.fromSystemProperties())
                    .stream()
                    .filter(violation -> violation.subject().equals(BothRetiredMarkers.class.getName()))
                    .toList();

            assertEquals(1, violations.size());
            assertEquals(
                    "retired-di-marker|" + BothRetiredMarkers.class.getName(),
                    violations.getFirst().debtKey()
            );
        } finally {
            if (previousRoots == null) {
                System.clearProperty("tavall.architecture.classRoots");
            } else {
                System.setProperty("tavall.architecture.classRoots", previousRoots);
            }
        }
    }

    interface RetiredContract extends IDependencyInjectableInterface {
    }

    static final class BothRetiredMarkers implements RetiredContract, IDependencyInjectableConcrete {
    }
}
