package org.tavall.minecraft.ffa.kit.npc;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class FFAKitNpcArchitectureTest {
    @Test
    public void commandInteractionAndRuntimeKeepTheirOwners() throws Exception {
        String facade = Files.readString(Path.of(
                "src/main/java/org/tavall/minecraft/ffa/command/FFAKitCommandFacade.java"
        ));
        String kitCommand = Files.readString(Path.of(
                "src/main/java/org/tavall/minecraft/ffa/command/FFAKitCommandHandler.java"
        ));
        String selection = Files.readString(Path.of(
                "src/main/java/org/tavall/minecraft/ffa/kit/selection/FFAKitSelectionHandler.java"
        ));
        String npcHandler = Files.readString(Path.of(
                "src/main/java/org/tavall/minecraft/ffa/kit/npc/handler/FFAKitNpcHandler.java"
        ));
        String interaction = Files.readString(Path.of(
                "src/main/java/org/tavall/minecraft/ffa/kit/npc/handler/FFAKitNpcInteractionHandler.java"
        ));
        String bootstrap = Files.readString(Path.of(
                "src/main/java/org/tavall/minecraft/ffa/bootstrap/FFAKitNpcBootstrap.java"
        ));
        String runtime = Files.readString(Path.of(
                "src/main/java/org/tavall/minecraft/ffa/runtime/FFARuntimeService.java"
        ));
        String descriptor = Files.readString(Path.of(
                "src/main/resources/META-INF/novus-modules/novus-ffa.json"
        ));

        assertTrue(facade.contains("kitCommandHandler.execute(sender, arguments)"));
        assertTrue(kitCommand.contains("novus.ffa.admin"));
        assertTrue(kitCommand.contains("/ffa kit apply <kit-name>"));
        assertTrue(kitCommand.contains("/ffa kit npc <kit-name>"));
        assertTrue(selection.contains("isAutoRespawnPending"));
        assertTrue(selection.contains("hasActiveKit"));
        assertTrue(selection.contains("runtimeService.respawn(player)"));
        assertTrue(selection.contains("implements AutoCloseable"));
        assertTrue(selection.contains("requireGeneration(selectionGeneration)"));
        assertTrue(npcHandler.contains("requireGeneration(mutationGeneration)"));
        assertTrue(interaction.contains("InteractionTargetType.NPC"));
        assertTrue(interaction.contains("select_ffa_kit"));
        assertTrue(bootstrap.contains("selectionHandler.close()"));
        assertTrue(bootstrap.contains("ffaPluginCommand.setExecutor(previousExecutor)"));
        assertTrue(runtime.contains("§eSelect a kit to enter FFA."));
        assertTrue(runtime.contains("autoRespawnHandler.cancel(player, this::waitAtSpawn)"));
        assertTrue(runtime.contains("§eSelect a kit to return to FFA."));
        assertTrue(descriptor.contains("FFACompositeBootstrap"));
    }
}
