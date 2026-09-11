package org.tavall.architecture.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CanonicalArchitectureTest {
    @Test
    void selectedCanonicalRulesExecuteAgainstConsumer() {
        ArchitectureReport report = ArchitectureAssessmentEngine.analyze(ArchitectureContext.fromSystemProperties());
        ArchitectureReportWriter.writeConfiguredReport(report);
        assertTrue(report.passed(), report::formatText);
    }
}
