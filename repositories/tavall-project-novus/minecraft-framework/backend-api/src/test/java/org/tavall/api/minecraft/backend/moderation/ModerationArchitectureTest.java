package org.tavall.api.minecraft.backend.moderation;

import org.junit.jupiter.api.Test;
import org.tavall.api.minecraft.backend.moderation.dialog.ModerationDialogDefinition;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ModerationArchitectureTest {
    @Test
    public void blacklistSurfaceDoesNotExist() {
        assertFalse(Arrays.stream(ModerationActionType.values())
                .map(Enum::name)
                .anyMatch(name -> name.contains("BLACKLIST")));
        assertFalse(Arrays.stream(ModerationPermission.values())
                .map(Enum::name)
                .anyMatch(name -> name.contains("BLACKLIST")));
    }

    @Test
    public void sharedContractsRemainPlatformNeutral() {
        assertTrue(ModerationAction.class.getPackageName()
                .startsWith("org.tavall.api.minecraft.backend.moderation"));
        assertTrue(ModerationDialogDefinition.class.getPackageName()
                .startsWith("org.tavall.api.minecraft.backend.moderation"));
        assertFalse(ModerationAction.class.getName().contains("velocity"));
        assertFalse(ModerationAction.class.getName().contains("discord"));
        assertFalse(ModerationDialogDefinition.class.getName().contains("paper"));
    }
}
