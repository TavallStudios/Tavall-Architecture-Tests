package org.tavall.architecture.core;

import java.util.Objects;

public record SourceLocation(
        String path,
        int startLine,
        int startColumn,
        int endLine,
        int endColumn
) {
    public SourceLocation {
        Objects.requireNonNull(path, "path");
        if (path.isBlank()) {
            throw new IllegalArgumentException("Source location path must be non-blank");
        }
        if (startLine < 1 || startColumn < 1 || endLine < 1 || endColumn < 1) {
            throw new IllegalArgumentException("Source location coordinates are one-based and must be positive");
        }
    }

    public static SourceLocation file(String path) {
        return new SourceLocation(path, 1, 1, 1, 1);
    }

    public String display() {
        return path + ":" + startLine + ":" + startColumn;
    }
}
