package org.tavall.architecture.core;

import java.util.ArrayList;
import java.util.List;

public final class CoreClassIntegrityRule implements ArchitectureRule {
    @Override
    public String id() {
        return "core-class-integrity";
    }

    @Override
    public List<ArchitectureViolation> validate(ArchitectureContext context) {
        List<ProductionClass> classes = context.productionClasses();
        List<ArchitectureViolation> violations = new ArrayList<>();
        if (classes.isEmpty()) {
            violations.add(new ArchitectureViolation(
                    "production-class-discovery",
                    "consumer",
                    "No compiled org.tavall production classes were discovered"
            ));
            return List.copyOf(violations);
        }
        for (ProductionClass productionClass : classes) {
            if (productionClass.origins().size() != 1) {
                violations.add(new ArchitectureViolation(
                        "duplicate-class",
                        productionClass.className(),
                        "Class is emitted by multiple production roots: " + productionClass.origins()
                ));
            }
            if (!productionClass.loaded()) {
                Throwable failure = productionClass.loadFailure();
                violations.add(new ArchitectureViolation(
                        "class-load",
                        productionClass.className(),
                        "Class could not load without initialization: "
                                + failure.getClass().getName() + ": " + String.valueOf(failure.getMessage())
                ));
            }
        }
        return List.copyOf(violations);
    }
}
