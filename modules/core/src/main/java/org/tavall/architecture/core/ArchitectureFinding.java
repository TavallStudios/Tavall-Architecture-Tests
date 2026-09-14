package org.tavall.architecture.core;

import java.util.Objects;

public record ArchitectureFinding(
        String familyId,
        String ruleId,
        String subject,
        String className,
        String message,
        ArchitectureSeverity severity,
        SourceLocation location,
        String policyReference
) {
    public ArchitectureFinding {
        Objects.requireNonNull(familyId, "familyId");
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(severity, "severity");
        if (familyId.isBlank() || ruleId.isBlank() || subject.isBlank()) {
            throw new IllegalArgumentException("Architecture finding identifiers must be non-blank");
        }
    }

    public ArchitectureFinding(
            String familyId,
            String ruleId,
            String subject,
            String className,
            String message,
            ArchitectureSeverity severity,
            SourceLocation location
    ) {
        this(familyId, ruleId, subject, className, message, severity, location, null);
    }

    public static ArchitectureFinding fromViolation(
            String familyId,
            ArchitectureViolation violation,
            ArchitectureContext context
    ) {
        String className = context.productionClassNameForSubject(violation.subject()).orElse(null);
        SourceLocation location = context.locateSubject(violation.subject()).orElse(null);
        return new ArchitectureFinding(
                familyId,
                violation.ruleId(),
                violation.subject(),
                className,
                violation.message(),
                ArchitectureSeverity.BLOCKING,
                location,
                null
        );
    }

    public ArchitectureViolation toViolation() {
        return new ArchitectureViolation(ruleId, subject, message);
    }

    public String debtKey() {
        return ruleId + "|" + subject;
    }
}
