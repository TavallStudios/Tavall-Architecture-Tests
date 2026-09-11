package org.tavall.architecture.cache;

import org.tavall.abstractcache.cache.AbstractCache;
import org.tavall.architecture.core.ArchitectureContext;
import org.tavall.architecture.core.ArchitectureRule;
import org.tavall.architecture.core.ArchitectureViolation;
import org.tavall.architecture.core.ProductionClass;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public final class CacheRule implements ArchitectureRule {
    @Override
    public String id() {
        return "tavall-cache";
    }

    @Override
    public List<ArchitectureViolation> validate(ArchitectureContext context) {
        List<ArchitectureViolation> violations = new ArrayList<>();
        for (ProductionClass productionClass : context.productionClasses()) {
            if (!productionClass.loaded()) {
                continue;
            }
            Class<?> type = productionClass.type();
            if (type.getSimpleName().endsWith("Cache")
                    && !type.isInterface()
                    && !Modifier.isAbstract(type.getModifiers())
                    && !AbstractCache.class.isAssignableFrom(type)) {
                violations.add(new ArchitectureViolation(
                        "cache-owner",
                        type.getName(),
                        "Concrete Tavall Cache types must use the shared Tavall Cache abstraction"
                ));
            }
        }
        return List.copyOf(violations);
    }
}
