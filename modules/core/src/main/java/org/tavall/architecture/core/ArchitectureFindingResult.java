package org.tavall.architecture.core;

import java.util.Objects;

public record ArchitectureFindingResult(ArchitectureFinding finding, boolean baselined) {
    public ArchitectureFindingResult {
        Objects.requireNonNull(finding, "finding");
    }
}
