package org.tavall.architecture.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CanonicalArchitectureTest {
    @Test
    void selectedCanonicalRulesExecuteAgainstConsumer() {
        ArchitectureContext context = ArchitectureContext.fromSystemProperties();
        List<ArchitectureRule> rules = ServiceLoader.load(ArchitectureRule.class, context.classLoader())
                .stream()
                .map(ServiceLoader.Provider::get)
                .toList();
        assertFalse(rules.isEmpty(), "No canonical architecture rules were discovered from selected modules");

        Map<String, ArchitectureViolation> observed = new LinkedHashMap<>();
        for (ArchitectureRule rule : rules) {
            for (ArchitectureViolation violation : rule.validate(context)) {
                ArchitectureViolation previous = observed.putIfAbsent(violation.debtKey(), violation);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate architecture debt key: " + violation.debtKey());
                }
            }
        }

        Set<String> debt = loadDebt();
        List<ArchitectureViolation> unexpected = observed.values().stream()
                .filter(violation -> !debt.contains(violation.debtKey()))
                .toList();
        Set<String> staleDebt = new LinkedHashSet<>(debt);
        staleDebt.removeAll(observed.keySet());

        assertTrue(unexpected.isEmpty(), () -> "New architecture violations:\n" + format(unexpected));
        assertTrue(staleDebt.isEmpty(), () -> "Architecture debt baseline can shrink; remove stale entries:\n"
                + String.join("\n", staleDebt));
    }

    private static Set<String> loadDebt() {
        String configured = System.getProperty("tavall.architecture.debtFile", "").strip();
        if (configured.isEmpty()) {
            return Set.of();
        }
        Path path = Path.of(configured);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Architecture debt file does not exist: " + path);
        }
        try {
            LinkedHashSet<String> debt = new LinkedHashSet<>();
            for (String raw : Files.readAllLines(path)) {
                String line = raw.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (!line.contains("|") || line.startsWith("|") || line.endsWith("|")) {
                    throw new IllegalStateException("Invalid architecture debt entry: " + line);
                }
                if (!debt.add(line)) {
                    throw new IllegalStateException("Duplicate architecture debt entry: " + line);
                }
            }
            return Set.copyOf(debt);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read architecture debt file " + path, exception);
        }
    }

    private static String format(List<ArchitectureViolation> violations) {
        List<String> lines = new ArrayList<>();
        for (ArchitectureViolation violation : violations) {
            lines.add(violation.debtKey() + " :: " + violation.message());
        }
        return String.join("\n", lines);
    }
}
