package org.tavall.architecture.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ArchitectureReportWriter {
    private ArchitectureReportWriter() {
    }

    public static void writeConfiguredReport(ArchitectureReport report) {
        String configured = System.getProperty("tavall.architecture.reportFile", "").strip();
        if (configured.isEmpty()) {
            return;
        }
        Path path = Path.of(configured).toAbsolutePath().normalize();
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, toJson(report));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write Tavall architecture report " + path, exception);
        }
    }

    public static String toJson(ArchitectureReport report) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        field(json, 1, "passed", report.passed()).append(",\n");
        json.append(indent(1)).append("\"selectedRuleFamilies\":");
        stringArray(json, report.selectedRuleFamilies()).append(",\n");
        json.append(indent(1)).append("\"classes\":[\n");
        for (int index = 0; index < report.classes().size(); index++) {
            ArchitectureClassAssessment assessment = report.classes().get(index);
            json.append(indent(2)).append("{\n");
            field(json, 3, "className", assessment.className()).append(",\n");
            field(json, 3, "status", assessment.status()).append(",\n");
            field(json, 3, "passed", assessment.passed()).append(",\n");
            field(json, 3, "score", assessment.score()).append(",\n");
            json.append(indent(3)).append("\"sourceLocation\":");
            location(json, assessment.sourceLocation()).append(",\n");
            json.append(indent(3)).append("\"findings\":");
            findings(json, assessment.findings(), 3).append('\n');
            json.append(indent(2)).append('}');
            if (index + 1 < report.classes().size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append(indent(1)).append("],\n");
        json.append(indent(1)).append("\"globalFindings\":");
        findings(json, report.globalFindings(), 1).append(",\n");
        json.append(indent(1)).append("\"staleDebt\":");
        stringArray(json, report.staleDebt()).append('\n');
        json.append("}\n");
        return json.toString();
    }

    private static StringBuilder findings(
            StringBuilder json,
            java.util.List<ArchitectureFindingResult> findings,
            int baseIndent
    ) {
        json.append("[\n");
        for (int index = 0; index < findings.size(); index++) {
            ArchitectureFindingResult result = findings.get(index);
            ArchitectureFinding finding = result.finding();
            json.append(indent(baseIndent + 1)).append("{\n");
            field(json, baseIndent + 2, "familyId", finding.familyId()).append(",\n");
            field(json, baseIndent + 2, "ruleId", finding.ruleId()).append(",\n");
            field(json, baseIndent + 2, "subject", finding.subject()).append(",\n");
            nullableField(json, baseIndent + 2, "className", finding.className()).append(",\n");
            field(json, baseIndent + 2, "message", finding.message()).append(",\n");
            field(json, baseIndent + 2, "severity", finding.severity().name()).append(",\n");
            nullableField(json, baseIndent + 2, "policyReference", finding.policyReference()).append(",\n");
            field(json, baseIndent + 2, "baselined", result.baselined()).append(",\n");
            json.append(indent(baseIndent + 2)).append("\"location\":");
            location(json, finding.location()).append('\n');
            json.append(indent(baseIndent + 1)).append('}');
            if (index + 1 < findings.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        return json.append(indent(baseIndent)).append(']');
    }

    private static StringBuilder location(StringBuilder json, SourceLocation location) {
        if (location == null) {
            return json.append("null");
        }
        json.append('{');
        json.append("\"path\":\"").append(escape(location.path())).append("\",");
        json.append("\"startLine\":").append(location.startLine()).append(',');
        json.append("\"startColumn\":").append(location.startColumn()).append(',');
        json.append("\"endLine\":").append(location.endLine()).append(',');
        json.append("\"endColumn\":").append(location.endColumn());
        return json.append('}');
    }

    private static StringBuilder field(StringBuilder json, int indent, String name, String value) {
        return json.append(indent(indent)).append('"').append(escape(name)).append("\":\"")
                .append(escape(value)).append('"');
    }

    private static StringBuilder nullableField(StringBuilder json, int indent, String name, String value) {
        json.append(indent(indent)).append('"').append(escape(name)).append("\":");
        return value == null ? json.append("null") : json.append('"').append(escape(value)).append('"');
    }

    private static StringBuilder field(StringBuilder json, int indent, String name, boolean value) {
        return json.append(indent(indent)).append('"').append(escape(name)).append("\":").append(value);
    }

    private static StringBuilder field(StringBuilder json, int indent, String name, int value) {
        return json.append(indent(indent)).append('"').append(escape(name)).append("\":").append(value);
    }

    private static StringBuilder stringArray(StringBuilder json, java.util.List<String> values) {
        json.append('[');
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append('"').append(escape(values.get(index))).append('"');
        }
        return json.append(']');
    }

    private static String indent(int level) {
        return "  ".repeat(level);
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
