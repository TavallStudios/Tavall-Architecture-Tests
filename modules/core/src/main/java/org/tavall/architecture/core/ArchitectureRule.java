package org.tavall.architecture.core;

import java.util.List;

public interface ArchitectureRule {
    String id();

    List<ArchitectureViolation> validate(ArchitectureContext context);
}
