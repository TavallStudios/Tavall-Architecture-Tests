package org.tavall.architecture;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TavallProductionClassArchitectureTest {
    private final TavallProductionClassIndex classIndex =
            new TavallProductionClassIndex();
    private final TavallArchitectureRuleSet ruleSet =
            new TavallArchitectureRuleSet();

    @Test
    void discoversCompiledTavallProductionClasses() {
        assertFalse(
                classIndex.discover().isEmpty(),
                "The architecture suite must discover production classes"
        );
    }

    @TestFactory
    Stream<DynamicTest> everyCompiledTavallClassSatisfiesApplicableRules() {
        return classIndex.discover().stream()
                .map(productionClass -> DynamicTest.dynamicTest(
                        productionClass.className(),
                        () -> {
                            List<TavallArchitectureRuleSet.Violation> violations =
                                    ruleSet.audit(productionClass);
                            assertTrue(
                                    violations.isEmpty(),
                                    () -> formatViolations(violations)
                            );
                        }
                ));
    }

    private String formatViolations(
            List<TavallArchitectureRuleSet.Violation> violations
    ) {
        return violations.stream()
                .map(violation -> violation.rule() + ": " + violation.message())
                .collect(java.util.stream.Collectors.joining(System.lineSeparator()));
    }
}
