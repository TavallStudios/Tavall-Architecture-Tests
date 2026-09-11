package org.tavall.architecture.core;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record ProductionClass(
        String className,
        List<Path> origins,
        Class<?> type,
        Throwable loadFailure
) {
    public ProductionClass {
        Objects.requireNonNull(className, "className");
        origins = List.copyOf(origins);
    }

    public boolean loaded() {
        return type != null && loadFailure == null;
    }
}
