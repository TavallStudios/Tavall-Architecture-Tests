package org.tavall.architecture.registry;

import org.tavall.architecture.core.ArchitectureContext;
import org.tavall.architecture.core.ArchitectureRule;
import org.tavall.architecture.core.ArchitectureViolation;
import org.tavall.architecture.core.ProductionClass;
import org.tavall.registry.IAbstractRegistry;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public final class RegistryRule implements ArchitectureRule {
    @Override
    public String id() {
        return "tavall-registry";
    }

    @Override
    public List<ArchitectureViolation> validate(ArchitectureContext context) {
        List<ArchitectureViolation> violations = new ArrayList<>();
        for (ProductionClass productionClass : context.productionClasses()) {
            if (!productionClass.loaded()) {
                continue;
            }
            Class<?> type = productionClass.type();
            if (type.getSimpleName().endsWith("Registry")
                    && !type.isInterface()
                    && !Modifier.isAbstract(type.getModifiers())
                    && !IAbstractRegistry.class.isAssignableFrom(type)) {
                violations.add(new ArchitectureViolation(
                        "registry-owner",
                        type.getName(),
                        "Concrete Tavall Registry types must use the shared Tavall Registry contract"
                ));
            }
        }
        return List.copyOf(violations);
    }
}
