package org.tavall.architecture.core;

import java.util.ArrayList;
import java.util.List;

public record ArchitectureReport(
        boolean passed,
        List<String> selectedRuleFamilies,
        List<ArchitectureClassAssessment> classes,
        List<ArchitectureFindingResult> globalFindings,
        List<String> staleDebt
) {
    public ArchitectureReport {
        selectedRuleFamilies = List.copyOf(selectedRuleFamilies);
        classes = List.copyOf(classes);
        globalFindings = List.copyOf(globalFindings);
        staleDebt = List.copyOf(staleDebt);
    }

    public String formatText() {
        List<String> lines = new ArrayList<>();
        lines.add("Tavall architecture assessment: " + (passed ? "PASS" : "FAIL"));
        lines.add("Inspected production classes: " + classes.size());
        for (ArchitectureClassAssessment assessment : classes) {
            String location = assessment.sourceLocation() == null
                    ? ""
                    : " @ " + assessment.sourceLocation().display();
            lines.add("[" + assessment.status() + " " + assessment.score() + "/100] "
                    + assessment.className() + location);
            for (ArchitectureFindingResult result : assessment.findings()) {
                ArchitectureFinding finding = result.finding();
                String findingLocation = finding.location() == null
                        ? ""
                        : " @ " + finding.location().display();
                String baseline = result.baselined() ? " [BASELINED]" : "";
                lines.add("  - " + finding.ruleId() + findingLocation + baseline + ": " + finding.message());
            }
        }
        if (!globalFindings.isEmpty()) {
            lines.add("Repository findings:");
            for (ArchitectureFindingResult result : globalFindings) {
                ArchitectureFinding finding = result.finding();
                String findingLocation = finding.location() == null
                        ? ""
                        : " @ " + finding.location().display();
                String baseline = result.baselined() ? " [BASELINED]" : "";
                lines.add("  - " + finding.debtKey() + findingLocation + baseline + ": " + finding.message());
            }
        }
        if (!staleDebt.isEmpty()) {
            lines.add("Architecture debt baseline can shrink; remove stale entries:");
            staleDebt.forEach(entry -> lines.add("  - " + entry));
        }
        return String.join("\n", lines);
    }
}
