package org.tavall.architecture.core;

import java.util.Objects;

public record SourceDiagnostic(String message, SourceLocation location) {
    public SourceDiagnostic {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(location, "location");
    }
}
