package org.tavall.architecture;

import org.junit.jupiter.api.Test;

import java.util.Set;

final class TavallPaperFullRuntimeSimulationTest {
    @Test
    void loadsAuditsAndClosesPaperRuntime() {
        new TavallFullRuntimeSimulationHarness().simulate(
                new TavallFullRuntimeSimulationHarness.RuntimeDefinition(
                        "paper",
                        Set.of(
                                "org.tavall.minecraft.server.BukkitServerPlugin"
                        ),
                        Set.of(
                                "org.tavall.commandmode.",
                                "org.tavall.minecraft.achievement.",
                                "org.tavall.minecraft.ffa.",
                                "org.tavall.minecraft.lobby.",
                                "org.tavall.minecraft.server."
                        )
                )
        );
    }
}
