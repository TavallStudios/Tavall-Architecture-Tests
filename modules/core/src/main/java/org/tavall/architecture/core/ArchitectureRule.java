package org.tavall.architecture.core;

import java.util.List;

public interface ArchitectureRule {
    String id();

    List<ArchitectureViolation> validate(ArchitectureContext context);

    default List<ArchitectureFinding> inspect(ArchitectureContext context) {
        return validate(context).stream()
                .map(violation -> ArchitectureFinding.fromViolation(id(), violation, context))
                .toList();
    }
}
