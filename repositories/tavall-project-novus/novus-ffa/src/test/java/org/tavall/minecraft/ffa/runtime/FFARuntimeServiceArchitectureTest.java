package org.tavall.minecraft.ffa.runtime;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.tavall.minecraft.ffa.combat.attribution.CombatAttributionRegistry;
import org.tavall.minecraft.ffa.combat.event.handler.interfaces.IFFACombatEventPublisher;
import org.tavall.minecraft.ffa.combat.focused.handler.FocusedEngagementRegistry;
import org.tavall.minecraft.ffa.combat.result.FFACombatResult;
import org.tavall.minecraft.ffa.combat.spawn.SpawnProtectionRegistry;
import org.tavall.minecraft.ffa.config.FFAConfiguration;
import org.tavall.minecraft.ffa.kit.handler.interfaces.IFFAKitHandler;
import org.tavall.minecraft.ffa.player.profile.handler.FFAPlayerProfileHandler;
import org.tavall.minecraft.ffa.player.settings.FFAPlayerSettings;
import org.tavall.minecraft.ffa.player.settings.FFARespawnMode;
import org.tavall.minecraft.ffa.player.settings.repository.interfaces.IFFAPlayerSettingsRepository;
import org.tavall.minecraft.ffa.rating.event.handler.interfaces.IFFARatingEventPublisher;
import org.tavall.minecraft.ffa.rating.handler.interfaces.IWeightedKillRatingHandler;
import org.tavall.minecraft.ffa.rating.repository.interfaces.IFFARatingTransactionRepository;
import org.tavall.minecraft.ffa.region.FFARegionControlService;
import org.tavall.minecraft.ffa.respawn.FFAAutoRespawnHandler;
import org.tavall.minecraft.ffa.round.handler.RoundMutationResult;
import org.tavall.minecraft.ffa.round.handler.interfaces.IFFARoundHandler;
import org.tavall.minecraft.ffa.spawn.handler.interfaces.IFFASpawnHandler;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FFARuntimeServiceArchitectureTest {
    @Test
    void facadeOwnsOnlyFocusedRuntimeHandlers() {
        Set<Class<?>> fieldTypes = Arrays.stream(
                        FFARuntimeService.class.getDeclaredFields()
                )
                .map(Field::getType)
                .collect(Collectors.toUnmodifiableSet());

        assertEquals(Set.of(
                FFAPlayerSessionHandler.class,
                FFAPlayerLifecycleHandler.class,
                FFACombatTransactionHandler.class,
                FFARoundTransitionHandler.class
        ), fieldTypes);
        assertFalse(fieldTypes.stream().anyMatch(Map.class::isAssignableFrom));
        assertFalse(fieldTypes.stream().anyMatch(
                java.util.Collection.class::isAssignableFrom
        ));
        assertFalse(fieldTypes.contains(AtomicInteger.class));
    }

    @Test
    void preservesPublicRuntimeMethods() throws Exception {
        assertNotNull(FFARuntimeService.class.getMethod(
                "attachProfileHandler",
                FFAPlayerProfileHandler.class
        ));
        assertNotNull(FFARuntimeService.class.getMethod("roundSnapshot"));
        assertNotNull(FFARuntimeService.class.getMethod("statistics"));
        assertNotNull(FFARuntimeService.class.getMethod(
                "settings",
                UUID.class
        ));
        assertNotNull(FFARuntimeService.class.getMethod("join", Player.class));
        assertNotNull(FFARuntimeService.class.getMethod(
                "pendingRespawnLocation",
                UUID.class
        ));
        assertNotNull(FFARuntimeService.class.getMethod(
                "isAutoRespawnCancelItem",
                ItemStack.class
        ));
        assertNotNull(FFARuntimeService.class.getMethod(
                "advanceRuntime",
                Instant.class,
                Duration.class
        ));
        assertNotNull(FFARuntimeService.class.getMethod(
                "applyCombatResult",
                FFACombatResult.class
        ));
        assertNotNull(FFARuntimeService.class.getMethod(
                "setFocused",
                UUID.class,
                boolean.class
        ));
        assertNotNull(FFARuntimeService.class.getMethod(
                "setRespawnMode",
                UUID.class,
                FFARespawnMode.class
        ));
        assertNotNull(FFARuntimeService.class.getMethod(
                "setSettings",
                UUID.class,
                FFAPlayerSettings.class
        ));
        assertNotNull(FFARuntimeService.class.getMethod(
                "pauseTimer"
        ));
        assertNotNull(FFARuntimeService.class.getMethod(
                "setTimer",
                Duration.class
        ));
        assertNotNull(FFARuntimeService.class.getMethod("resetRound"));
        assertNotNull(FFARuntimeService.class.getMethod("close"));
    }

    @Test
    void preservesAllConstructorOverloads() throws Exception {
        Class<?>[] common = {
                JavaPlugin.class,
                FFAConfiguration.class,
                IFFARoundHandler.class,
                IFFAKitHandler.class,
                IFFASpawnHandler.class,
                SpawnProtectionRegistry.class,
                FocusedEngagementRegistry.class,
                CombatAttributionRegistry.class,
                IFFAPlayerSettingsRepository.class,
                IFFARatingTransactionRepository.class,
                IWeightedKillRatingHandler.class,
                IFFARatingEventPublisher.class
        };

        assertNotNull(FFARuntimeService.class.getConstructor(concat(
                common,
                Consumer.class,
                Consumer.class
        )));
        assertNotNull(FFARuntimeService.class.getConstructor(concat(
                common,
                FFAAutoRespawnHandler.class,
                FFARegionControlService.class,
                Consumer.class,
                Consumer.class
        )));
        assertNotNull(FFARuntimeService.class.getConstructor(concat(
                common,
                IFFACombatEventPublisher.class,
                FFAAutoRespawnHandler.class,
                FFARegionControlService.class,
                Consumer.class,
                Consumer.class
        )));
    }

    private Class<?>[] concat(Class<?>[] prefix, Class<?>... suffix) {
        Class<?>[] result = Arrays.copyOf(prefix, prefix.length + suffix.length);
        System.arraycopy(suffix, 0, result, prefix.length, suffix.length);
        return result;
    }
}
