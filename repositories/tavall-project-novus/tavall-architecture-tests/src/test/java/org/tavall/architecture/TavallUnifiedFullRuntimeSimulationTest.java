package org.tavall.architecture;

import org.junit.jupiter.api.Test;

import java.util.Set;

final class TavallUnifiedFullRuntimeSimulationTest {
    @Test
    void loadsAuditsAndClosesUnifiedRuntime() {
        new TavallFullRuntimeSimulationHarness().simulate(
                new TavallFullRuntimeSimulationHarness.RuntimeDefinition(
                        "unified",
                        Set.of(
                                "org.tavall.runtime.TavallRuntimeMain"
                        ),
                        Set.of("org.tavall.runtime.")
                )
        );
    }
}
