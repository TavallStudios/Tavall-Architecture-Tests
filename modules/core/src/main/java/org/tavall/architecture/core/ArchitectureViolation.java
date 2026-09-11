package org.tavall.architecture.core;

import java.util.Objects;

public record ArchitectureViolation(String ruleId, String subject, String message) {
    public ArchitectureViolation {
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(message, "message");
        if (ruleId.isBlank() || subject.isBlank()) {
            throw new IllegalArgumentException("Architecture violation ruleId and subject must be non-blank");
        }
    }

    public String debtKey() {
        return ruleId + "|" + subject;
    }
}
