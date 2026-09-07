package org.tavall.architecture;

import org.junit.jupiter.api.Test;

import java.util.Set;

final class TavallWebFullRuntimeSimulationTest {
    @Test
    void loadsAuditsAndClosesWebRuntime() {
        new TavallFullRuntimeSimulationHarness().simulate(
                new TavallFullRuntimeSimulationHarness.RuntimeDefinition(
                        "web",
                        Set.of(
                                "org.tavall.novus.web.NovusWebApplication"
                        ),
                        Set.of("org.tavall.novus.web.")
                )
        );
    }
}
