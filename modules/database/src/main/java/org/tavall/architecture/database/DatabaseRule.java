package org.tavall.architecture.database;

import org.tavall.architecture.core.ArchitectureContext;
import org.tavall.architecture.core.ArchitectureRule;
import org.tavall.architecture.core.ArchitectureViolation;
import org.tavall.architecture.core.ProductionClass;

import java.util.ArrayList;
import java.util.List;

public final class DatabaseRule implements ArchitectureRule {
    @Override
    public String id() {
        return "tavall-database";
    }

    @Override
    public List<ArchitectureViolation> validate(ArchitectureContext context) {
        List<ArchitectureViolation> violations = new ArrayList<>();
        for (ProductionClass productionClass : context.productionClasses()) {
            if (!productionClass.loaded()) {
                continue;
            }
            String name = productionClass.type().getSimpleName();
            if (name.endsWith("Repository") || name.endsWith("RepositoryImpl")
                    || name.endsWith("RepositoryAdapter") || name.endsWith("RepositoryStore")) {
                violations.add(new ArchitectureViolation(
                        "database-repository-layer",
                        productionClass.className(),
                        "Application repository layers are prohibited; use Tavall Database or name the actual capability"
                ));
            }
        }
        return List.copyOf(violations);
    }
}
