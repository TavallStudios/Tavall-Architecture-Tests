package org.tavall.architecture;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Reusable cross-repository architecture contracts for Tavall control and
 * integration surfaces.
 *
 * <p>Consumer repositories can feed these contracts their real command,
 * catalog, and pull-request snapshots. The contracts deliberately do not
 * know a repository's implementation classes or filesystem layout.</p>
 */
final class TavallArchitectureContracts {
    private static final String MAIN = "main";

    private TavallArchitectureContracts() {
    }

    static void requireNoPublicWorkspaceAuthority(
            Collection<String> publicCapabilityNames,
            Collection<String> publicParameterNames
    ) {
        Set<String> forbidden = new LinkedHashSet<>();
        addForbidden(publicCapabilityNames, forbidden);
        addForbidden(publicParameterNames, forbidden);
        if (!forbidden.isEmpty()) {
            throw new IllegalStateException(
                    "Public development authority exposes workspace or lease identity: " + forbidden
            );
        }
    }

    static void requireMcpProjection(
            Collection<String> publicMcpNames,
            Collection<String> nativeMcpNames,
            Collection<String> cliCapabilityNames
    ) {
        Set<String> publicNames = normalize(publicMcpNames, "public MCP names");
        Set<String> nativeNames = normalize(nativeMcpNames, "native MCP names");
        Set<String> cliNames = normalize(cliCapabilityNames, "CLI capability names");

        Set<String> outsideNative = new LinkedHashSet<>(publicNames);
        outsideNative.removeAll(nativeNames);
        if (!outsideNative.isEmpty()) {
            throw new IllegalStateException(
                    "MCP publishes capabilities outside its native bootstrap set: " + outsideNative
            );
        }

        Set<String> cliMirrors = new LinkedHashSet<>(publicNames);
        cliMirrors.retainAll(cliNames);
        if (!cliMirrors.isEmpty()) {
            throw new IllegalStateException(
                    "MCP republishes CLI capabilities: " + cliMirrors
            );
        }
        requireNoPublicWorkspaceAuthority(publicMcpNames, Set.of());
    }

    static void requireCommandProjection(
            Collection<String> acceptedCommandForms,
            Collection<String> advertisedCommandForms
    ) {
        Set<String> accepted = normalize(acceptedCommandForms, "accepted command forms");
        Set<String> advertised = normalize(advertisedCommandForms, "advertised command forms");
        if (!accepted.equals(advertised)) {
            Set<String> missing = new LinkedHashSet<>(accepted);
            missing.removeAll(advertised);
            Set<String> extra = new LinkedHashSet<>(advertised);
            extra.removeAll(accepted);
            throw new IllegalStateException(
                    "CLI command registry/help drift: missing=" + missing + ", extra=" + extra
            );
        }
    }

    static void requireStagingAncestry(
            Map<String, String> parentByPullRequest,
            Collection<String> activeNormalPullRequests,
            Collection<String> activeStagingPullRequests,
            Collection<String> repositoryStagingRoots
    ) {
        Objects.requireNonNull(parentByPullRequest, "parentByPullRequest");
        Set<String> normal = normalize(activeNormalPullRequests, "active normal pull requests");
        Set<String> staging = normalize(activeStagingPullRequests, "active staging pull requests");
        Set<String> roots = normalize(repositoryStagingRoots, "repository staging roots");
        Set<String> overlap = new LinkedHashSet<>(normal);
        overlap.retainAll(staging);
        if (!overlap.isEmpty()) {
            throw new IllegalStateException("A pull request cannot be both normal and staging: " + overlap);
        }
        if (roots.size() != 1) {
            throw new IllegalStateException(
                    "Every integration scope requires one unambiguous active staging root: " + roots
            );
        }
        if (!staging.containsAll(roots)) {
            Set<String> missingRoots = new LinkedHashSet<>(roots);
            missingRoots.removeAll(staging);
            throw new IllegalStateException("Staging roots are not active staging pull requests: " + missingRoots);
        }

        Set<String> active = new LinkedHashSet<>(normal);
        active.addAll(staging);
        String root = roots.iterator().next();
        for (String pullRequest : active) {
            if (pullRequest.equals(root)) {
                continue;
            }
            Set<String> seen = new LinkedHashSet<>();
            String current = pullRequest;
            while (true) {
                if (!seen.add(current)) {
                    throw new IllegalStateException(
                            "Staging ancestry contains a cycle: " + seen
                    );
                }
                String parent = normalizeOne(parentByPullRequest.get(current), "parent of " + current);
                if (parent.equals(MAIN)) {
                    throw new IllegalStateException(
                            "Active pull request bypasses staging and targets main: " + pullRequest
                    );
                }
                if (parent.equals(root)) {
                    break;
                }
                if (!active.contains(parent)) {
                    throw new IllegalStateException(
                            "Active pull request has an orphan staging ancestor: "
                                    + pullRequest + " -> " + parent
                    );
                }
                current = parent;
            }
        }
    }

    private static void addForbidden(Collection<String> values, Set<String> forbidden) {
        Objects.requireNonNull(values, "public names");
        for (String value : values) {
            String normalized = normalizeOne(value, "public names");
            if (isForbiddenPublicIdentity(value)) forbidden.add(normalized);
        }
    }

    private static boolean isForbiddenPublicIdentity(String value) {
        String normalized = value.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return normalized.contains("workspace")
                || normalized.contains("work_path")
                || normalized.equals("workpath")
                || normalized.matches("(?:.*_)?lease(?:id|generation|token)?(?:_.*)?");
    }

    private static Set<String> normalize(Collection<String> values, String label) {
        Objects.requireNonNull(values, label);
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            normalized.add(normalizeOne(value, label));
        }
        return Set.copyOf(normalized);
    }

    private static String normalizeOne(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " contains a blank value");
        }
        return value.strip().toLowerCase(Locale.ROOT);
    }
}
