package org.tavall.architecture;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TavallArchitectureContractsTest {
    @Test
    void acceptsNativeMcpAndExactCliProjectionsWithoutWorkspaceIdentity() {
        assertDoesNotThrow(() -> TavallArchitectureContracts.requireMcpProjection(
                Set.of("cloud_catalog_list", "cloud_status"),
                Set.of("cloud_catalog_list", "cloud_status", "cloud_create_sandbox"),
                Set.of("environment repository read", "environment repository git status")
        ));
        assertDoesNotThrow(() -> TavallArchitectureContracts.requireCommandProjection(
                List.of("status", "environment repository staging validate"),
                List.of("status", "environment repository staging validate")
        ));
    }

    @Test
    void rejectsPublicWorkspaceAndLeaseIdentities() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> TavallArchitectureContracts.requireNoPublicWorkspaceAuthority(
                        Set.of("cloud_dev_environment_repository_read"),
                        Set.of("environmentId", "workspaceId")
                )
        );
        assertTrue(exception.getMessage().contains("workspace"));
    }

    @Test
    void rejectsMcpCliMirrorsAndCapabilitiesOutsideNativeSet() {
        IllegalStateException mirror = assertThrows(
                IllegalStateException.class,
                () -> TavallArchitectureContracts.requireMcpProjection(
                        Set.of("cloud_status", "environment repository read"),
                        Set.of("cloud_status", "environment repository read"),
                        Set.of("environment repository read")
                )
        );
        assertTrue(mirror.getMessage().contains("CLI"));

        IllegalStateException outsideNative = assertThrows(
                IllegalStateException.class,
                () -> TavallArchitectureContracts.requireMcpProjection(
                        Set.of("cloud_status", "cloud_dev_environment_repository_exec"),
                        Set.of("cloud_status"),
                        Set.of()
                )
        );
        assertTrue(outsideNative.getMessage().contains("native"));
    }

    @Test
    void rejectsCommandHelpDrift() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> TavallArchitectureContracts.requireCommandProjection(
                        Set.of("status", "environment repository staging validate"),
                        Set.of("status", "environment repository staging discover")
                )
        );
        assertTrue(exception.getMessage().contains("drift"));
    }

    @Test
    void acceptsTransitiveNormalAndSubStagingAncestry() {
        assertDoesNotThrow(() -> TavallArchitectureContracts.requireStagingAncestry(
                Map.of(
                        "feature", "domain",
                        "domain", "release",
                        "release", "main"
                ),
                Set.of("feature"),
                Set.of("domain", "release"),
                Set.of("release")
        ));
    }

    @Test
    void rejectsDirectMainAndOrphanAncestry() {
        IllegalStateException directMain = assertThrows(
                IllegalStateException.class,
                () -> TavallArchitectureContracts.requireStagingAncestry(
                        Map.of("feature", "main", "release", "main"),
                        Set.of("feature"),
                        Set.of("release"),
                        Set.of("release")
                )
        );
        assertTrue(directMain.getMessage().contains("bypasses"));

        IllegalStateException orphan = assertThrows(
                IllegalStateException.class,
                () -> TavallArchitectureContracts.requireStagingAncestry(
                        Map.of("feature", "missing", "release", "main"),
                        Set.of("feature"),
                        Set.of("release"),
                        Set.of("release")
                )
        );
        assertTrue(orphan.getMessage().contains("orphan"));
    }

    @Test
    void rejectsCyclesClosedParentsAndAmbiguousRoots() {
        IllegalStateException cycle = assertThrows(
                IllegalStateException.class,
                () -> TavallArchitectureContracts.requireStagingAncestry(
                        Map.of("feature", "domain", "domain", "feature", "release", "main"),
                        Set.of("feature"),
                        Set.of("domain", "release"),
                        Set.of("release")
                )
        );
        assertTrue(cycle.getMessage().contains("cycle"));

        IllegalStateException closedParent = assertThrows(
                IllegalStateException.class,
                () -> TavallArchitectureContracts.requireStagingAncestry(
                        Map.of("feature", "closed-parent", "release", "main"),
                        Set.of("feature"),
                        Set.of("release"),
                        Set.of("release")
                )
        );
        assertTrue(closedParent.getMessage().contains("orphan"));

        IllegalStateException ambiguousRoots = assertThrows(
                IllegalStateException.class,
                () -> TavallArchitectureContracts.requireStagingAncestry(
                        Map.of("release-a", "main", "release-b", "main"),
                        Set.of(),
                        Set.of("release-a", "release-b"),
                        Set.of("release-a", "release-b")
                )
        );
        assertTrue(ambiguousRoots.getMessage().contains("one unambiguous"));
    }
}
