package org.tavall.architecture;

import org.junit.jupiter.api.Test;

import java.util.Set;

final class TavallVelocityFullRuntimeSimulationTest {
    @Test
    void loadsAuditsAndClosesVelocityRuntime() {
        new TavallFullRuntimeSimulationHarness().simulate(
                new TavallFullRuntimeSimulationHarness.RuntimeDefinition(
                        "velocity",
                        Set.of(
                                "org.tavall.minecraft.bootstrap.VelocityProxyPlugin"
                        ),
                        Set.of(
                                "org.tavall.minecraft.bootstrap.",
                                "org.tavall.minecraft.commands.",
                                "org.tavall.minecraft.essentials.velocity.",
                                "org.tavall.minecraft.identity.",
                                "org.tavall.minecraft.resourcepack."
                        )
                )
        );
    }
}
