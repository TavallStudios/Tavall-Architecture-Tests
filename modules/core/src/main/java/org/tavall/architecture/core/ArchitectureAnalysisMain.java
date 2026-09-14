package org.tavall.architecture.core;

public final class ArchitectureAnalysisMain {
    private ArchitectureAnalysisMain() {
    }

    public static void main(String[] args) {
        ArchitectureReport report = ArchitectureAssessmentEngine.analyze(ArchitectureContext.fromSystemProperties());
        ArchitectureReportWriter.writeConfiguredReport(report);
        System.out.println(report.formatText());
        if (!report.passed()) {
            System.exit(1);
        }
    }
}
