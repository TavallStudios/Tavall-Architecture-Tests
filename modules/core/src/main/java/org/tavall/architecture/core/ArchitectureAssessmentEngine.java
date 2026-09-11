package org.tavall.architecture.core;

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

public final class ArchitectureAssessmentEngine {
    private ArchitectureAssessmentEngine() {
    }

    public static ArchitectureReport analyze(ArchitectureContext context) {
        List<ArchitectureRule> rules = ServiceLoader.load(ArchitectureRule.class, context.classLoader())
                .stream()
                .map(ServiceLoader.Provider::get)
                .toList();
        return analyze(context, rules);
    }

    public static ArchitectureReport analyze(ArchitectureContext context, List<ArchitectureRule> rules) {
        if (rules.isEmpty()) {
            throw new IllegalStateException("No canonical architecture rules were discovered from selected modules");
        }
        List<String> families = rules.stream().map(ArchitectureRule::id).distinct().sorted().toList();
        Map<String, ArchitectureFinding> observed = new LinkedHashMap<>();
        for (ArchitectureRule rule : rules) {
            for (ArchitectureFinding finding : rule.inspect(context)) {
                ArchitectureFinding previous = observed.putIfAbsent(finding.debtKey(), finding);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate architecture debt key: " + finding.debtKey());
                }
            }
        }

        Set<String> debt = loadDebt();
        List<ArchitectureFindingResult> findingResults = observed.values().stream()
                .map(finding -> new ArchitectureFindingResult(finding, debt.contains(finding.debtKey())))
                .toList();
        Set<String> stale = new LinkedHashSet<>(debt);
        stale.removeAll(observed.keySet());

        List<ArchitectureClassAssessment> classAssessments = new ArrayList<>();
        for (ProductionClass productionClass : context.productionClasses()) {
            List<ArchitectureFindingResult> classFindings = findingResults.stream()
                    .filter(result -> productionClass.className().equals(result.finding().className()))
                    .toList();
            boolean classPassed = classFindings.stream().noneMatch(result ->
                    !result.baselined() && result.finding().severity().blocking()
            );
            Set<String> failedFamilies = new LinkedHashSet<>();
            classFindings.forEach(result -> failedFamilies.add(result.finding().familyId()));
            int score = complianceScore(families.size(), failedFamilies.size());
            classAssessments.add(new ArchitectureClassAssessment(
                    productionClass.className(),
                    context.productionSources().locateClass(productionClass.className()).orElse(null),
                    classPassed,
                    score,
                    families,
                    classFindings
            ));
        }

        List<ArchitectureFindingResult> global = findingResults.stream()
                .filter(result -> result.finding().className() == null)
                .toList();
        boolean globalPassed = global.stream().noneMatch(result ->
                !result.baselined() && result.finding().severity().blocking()
        );
        boolean passed = classAssessments.stream().allMatch(ArchitectureClassAssessment::passed)
                && globalPassed
                && stale.isEmpty();
        return new ArchitectureReport(passed, families, classAssessments, global, List.copyOf(stale));
    }

    private static int complianceScore(int selectedFamilies, int failedFamilies) {
        if (selectedFamilies <= 0 || failedFamilies <= 0) {
            return 100;
        }
        int boundedFailures = Math.min(selectedFamilies, failedFamilies);
        return (int) Math.round(100.0d * (selectedFamilies - boundedFailures) / selectedFamilies);
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
}
