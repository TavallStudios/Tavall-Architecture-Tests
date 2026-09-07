package org.tavall.architecture;

import org.junit.jupiter.api.Test;

import java.util.Set;

final class TavallDiscordFullRuntimeSimulationTest {
    @Test
    void loadsAuditsAndClosesDiscordRuntime() {
        new TavallFullRuntimeSimulationHarness().simulate(
                new TavallFullRuntimeSimulationHarness.RuntimeDefinition(
                        "discord",
                        Set.of(
                                "org.tavall.discord.core.NovusDiscordCoreApplication"
                        ),
                        Set.of("org.tavall.discord.")
                )
        );
    }
}
