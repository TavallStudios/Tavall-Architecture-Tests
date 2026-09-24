package org.tavall.architecture.core;

import java.util.List;
import java.util.Objects;

public record ArchitectureClassAssessment(
        String className,
        SourceLocation sourceLocation,
        boolean passed,
        int score,
        List<String> selectedRuleFamilies,
        List<ArchitectureFindingResult> findings
) {
    public ArchitectureClassAssessment {
        Objects.requireNonNull(className, "className");
        selectedRuleFamilies = List.copyOf(selectedRuleFamilies);
        findings = List.copyOf(findings);
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Architecture class score must be between 0 and 100");
        }
    }

    public String status() {
        if (!passed) {
            return "FAIL";
        }
        return findings.stream().anyMatch(ArchitectureFindingResult::baselined) ? "DEBT" : "PASS";
    }
}
