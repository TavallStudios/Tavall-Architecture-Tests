package org.tavall.minecraft.ffa.bootstrap;

import org.junit.jupiter.api.Test;
import org.tavall.dependency.IDependencyInjectableConcrete;
import org.tavall.dependency.IDependencyInjectableInterface;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.minecraft.ffa.combat.attribution.handler.CombatEvidenceRatingHandler;
import org.tavall.minecraft.ffa.combat.attribution.handler.interfaces.ICombatEvidenceRatingHandler;
import org.tavall.minecraft.ffa.combat.cleanup.handler.CleanupProtectionHandler;
import org.tavall.minecraft.ffa.combat.cleanup.handler.interfaces.ICleanupProtectionHandler;
import org.tavall.minecraft.ffa.combat.focused.handler.PaperFocusedFfaVisibilityGateway;
import org.tavall.minecraft.ffa.combat.focused.handler.interfaces.IFocusedFfaVisibilityGateway;
import org.tavall.minecraft.ffa.kit.handler.BukkitFFAKitHandler;
import org.tavall.minecraft.ffa.kit.handler.interfaces.IFFAKitHandler;
import org.tavall.minecraft.ffa.player.settings.repository.PostgresFFAPlayerSettingsRepository;
import org.tavall.minecraft.ffa.player.settings.repository.interfaces.IFFAPlayerSettingsRepository;
import org.tavall.minecraft.ffa.rating.event.handler.FFARatingEventPublisher;
import org.tavall.minecraft.ffa.rating.event.handler.interfaces.IFFARatingEventPublisher;
import org.tavall.minecraft.ffa.rating.glicko.handler.Glicko2CalculationHandler;
import org.tavall.minecraft.ffa.rating.glicko.handler.interfaces.IGlicko2CalculationHandler;
import org.tavall.minecraft.ffa.rating.grading.handler.PersistentGradeHandler;
import org.tavall.minecraft.ffa.rating.grading.handler.RoundPerformanceGradeHandler;
import org.tavall.minecraft.ffa.rating.grading.handler.interfaces.IPersistentGradeHandler;
import org.tavall.minecraft.ffa.rating.grading.handler.interfaces.IRoundPerformanceGradeHandler;
import org.tavall.minecraft.ffa.rating.handler.WeightedKillRatingHandler;
import org.tavall.minecraft.ffa.rating.handler.interfaces.IWeightedKillRatingHandler;
import org.tavall.minecraft.ffa.rating.placement.handler.OpenPlacementRatingHandler;
import org.tavall.minecraft.ffa.rating.placement.handler.interfaces.IOpenPlacementRatingHandler;
import org.tavall.minecraft.ffa.rating.repository.PostgresFFAPlacementTransactionRepository;
import org.tavall.minecraft.ffa.rating.repository.PostgresFFARatingTransactionRepository;
import org.tavall.minecraft.ffa.rating.repository.interfaces.IFFAPlacementTransactionRepository;
import org.tavall.minecraft.ffa.rating.repository.interfaces.IFFARatingTransactionRepository;
import org.tavall.minecraft.ffa.rating.simulation.handler.FFASimulationHandler;
import org.tavall.minecraft.ffa.rating.simulation.handler.interfaces.IFFASimulationHandler;
import org.tavall.minecraft.ffa.round.handler.FFARoundFinalizationHandler;
import org.tavall.minecraft.ffa.round.handler.FFARoundHandler;
import org.tavall.minecraft.ffa.round.handler.interfaces.IFFARoundFinalizationHandler;
import org.tavall.minecraft.ffa.round.handler.interfaces.IFFARoundHandler;
import org.tavall.minecraft.ffa.round.orchestrator.FFARoundOrchestrator;
import org.tavall.minecraft.ffa.round.orchestrator.interfaces.IFFARoundOrchestrator;
import org.tavall.minecraft.ffa.round.repository.PostgresFFAMatchRepository;
import org.tavall.minecraft.ffa.round.repository.interfaces.IFFAMatchRepository;
import org.tavall.minecraft.ffa.round.router.FFARoundTransitionRouter;
import org.tavall.minecraft.ffa.round.router.interfaces.IFFARoundTransitionRouter;
import org.tavall.minecraft.ffa.round.runtime.handler.interfaces.IFFARoundRuntimeHandler;
import org.tavall.minecraft.ffa.round.scheduler.PaperFFARoundTaskScheduler;
import org.tavall.minecraft.ffa.round.scheduler.interfaces.IFFARoundTaskScheduler;
import org.tavall.minecraft.ffa.runtime.FFARuntimeService;
import org.tavall.minecraft.ffa.spawn.handler.BukkitFFASpawnHandler;
import org.tavall.minecraft.ffa.spawn.handler.interfaces.IFFASpawnHandler;
import org.tavall.minecraft.nms.visibility.handler.NoOpPlayerTranslucencyProjectionHandler;
import org.tavall.minecraft.nms.visibility.handler.ReflectivePaperPlayerTranslucencyProjectionHandler;
import org.tavall.minecraft.nms.visibility.handler.interfaces.IPlayerTranslucencyProjectionHandler;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FFADependencyArchitectureTest {
    @Test
    void contractsAndDelegatesFollowTavallDiBindings() {
        Map<Class<?>, Class<?>> delegates = Map.ofEntries(
                Map.entry(
                        PostgresFFARatingTransactionRepository.class,
                        IFFARatingTransactionRepository.class
                ),
                Map.entry(
                        PostgresFFAPlacementTransactionRepository.class,
                        IFFAPlacementTransactionRepository.class
                ),
                Map.entry(PostgresFFAMatchRepository.class, IFFAMatchRepository.class),
                Map.entry(
                        PostgresFFAPlayerSettingsRepository.class,
                        IFFAPlayerSettingsRepository.class
                ),
                Map.entry(Glicko2CalculationHandler.class, IGlicko2CalculationHandler.class),
                Map.entry(WeightedKillRatingHandler.class, IWeightedKillRatingHandler.class),
                Map.entry(FFARatingEventPublisher.class, IFFARatingEventPublisher.class),
                Map.entry(OpenPlacementRatingHandler.class, IOpenPlacementRatingHandler.class),
                Map.entry(PersistentGradeHandler.class, IPersistentGradeHandler.class),
                Map.entry(
                        RoundPerformanceGradeHandler.class,
                        IRoundPerformanceGradeHandler.class
                ),
                Map.entry(CombatEvidenceRatingHandler.class, ICombatEvidenceRatingHandler.class),
                Map.entry(CleanupProtectionHandler.class, ICleanupProtectionHandler.class),
                Map.entry(
                        PaperFocusedFfaVisibilityGateway.class,
                        IFocusedFfaVisibilityGateway.class
                ),
                Map.entry(
                        ReflectivePaperPlayerTranslucencyProjectionHandler.class,
                        IPlayerTranslucencyProjectionHandler.class
                ),
                Map.entry(
                        NoOpPlayerTranslucencyProjectionHandler.class,
                        IPlayerTranslucencyProjectionHandler.class
                ),
                Map.entry(BukkitFFAKitHandler.class, IFFAKitHandler.class),
                Map.entry(FFARoundHandler.class, IFFARoundHandler.class),
                Map.entry(BukkitFFASpawnHandler.class, IFFASpawnHandler.class),
                Map.entry(
                        FFARoundFinalizationHandler.class,
                        IFFARoundFinalizationHandler.class
                ),
                Map.entry(FFARoundOrchestrator.class, IFFARoundOrchestrator.class),
                Map.entry(FFARoundTransitionRouter.class, IFFARoundTransitionRouter.class),
                Map.entry(PaperFFARoundTaskScheduler.class, IFFARoundTaskScheduler.class),
                Map.entry(FFARuntimeService.class, IFFARoundRuntimeHandler.class),
                Map.entry(FFASimulationHandler.class, IFFASimulationHandler.class)
        );
        delegates.forEach((implementation, contract) -> {
            assertTrue(IDependencyInjectableInterface.class.isAssignableFrom(contract));
            assertTrue(IDependencyInjectableConcrete.class.isAssignableFrom(implementation));
            DelegatesTo annotation = implementation.getAnnotation(DelegatesTo.class);
            assertNotNull(annotation);
            assertTrue(
                    Arrays.asList(annotation.value()).contains(contract),
                    () -> implementation.getName() + " must delegate to " + contract.getName()
            );
        });
    }
}
