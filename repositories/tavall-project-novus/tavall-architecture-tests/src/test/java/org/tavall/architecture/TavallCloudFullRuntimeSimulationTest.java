package org.tavall.architecture;

import org.junit.jupiter.api.Test;

import java.util.Set;

final class TavallCloudFullRuntimeSimulationTest {
    @Test
    void loadsAuditsAndClosesCloudIngressRuntime() {
        new TavallFullRuntimeSimulationHarness().simulate(
                new TavallFullRuntimeSimulationHarness.RuntimeDefinition(
                        "cloud-ingress",
                        Set.of(
                                "org.tavall.minecraft.cloud.proxy.transport.MinecraftTcpIngressApplication"
                        ),
                        Set.of("org.tavall.minecraft.cloud.")
                )
        );
    }
}
