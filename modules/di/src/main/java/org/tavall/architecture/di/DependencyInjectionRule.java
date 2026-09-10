package org.tavall.architecture.di;

import org.tavall.architecture.core.ArchitectureContext;
import org.tavall.architecture.core.ArchitectureRule;
import org.tavall.architecture.core.ArchitectureViolation;
import org.tavall.architecture.core.ProductionClass;
import org.tavall.dependency.annotations.DelegatesTo;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DependencyInjectionRule implements ArchitectureRule {
    private static final Set<String> RETIRED_DI_MARKERS = Set.of(
            "org.tavall.dependency.IDependencyInjectableConcrete",
            "org.tavall.dependency.IDependencyInjectableInterface"
    );

    @Override
    public String id() {
        return "tavall-di";
    }

    @Override
    public List<ArchitectureViolation> validate(ArchitectureContext context) {
        List<ArchitectureViolation> violations = new ArrayList<>();
        for (ProductionClass productionClass : context.productionClasses()) {
            if (!productionClass.loaded()) {
                continue;
            }
            Class<?> type = productionClass.type();
            for (String marker : findRetiredMarkers(type, new HashSet<>())) {
                violations.add(new ArchitectureViolation(
                        "retired-di-marker",
                        type.getName(),
                        "Production type still depends on retired DI marker " + marker
                ));
            }
            DelegatesTo delegatesTo = type.getAnnotation(DelegatesTo.class);
            if (delegatesTo != null) {
                auditDelegation(type, delegatesTo, violations);
            }
        }
        return List.copyOf(violations);
    }

    private static void auditDelegation(
            Class<?> concreteType,
            DelegatesTo delegatesTo,
            List<ArchitectureViolation> violations
    ) {
        if (concreteType.isInterface() || Modifier.isAbstract(concreteType.getModifiers())) {
            violations.add(new ArchitectureViolation(
                    "delegation-concrete",
                    concreteType.getName(),
                    "@DelegatesTo is declared on a type that is not concrete"
            ));
        }
        Set<Class<?>> aliases = new HashSet<>();
        Arrays.stream(delegatesTo.value()).forEach(alias -> {
            if (!aliases.add(alias)) {
                violations.add(new ArchitectureViolation(
                        "delegation-duplicate",
                        concreteType.getName() + "->" + alias.getName(),
                        "Delegation alias is repeated"
                ));
            }
            if (!alias.isAssignableFrom(concreteType)) {
                violations.add(new ArchitectureViolation(
                        "delegation-assignability",
                        concreteType.getName() + "->" + alias.getName(),
                        "Concrete type cannot satisfy delegation alias"
                ));
            }
        });
    }

    private static Set<String> findRetiredMarkers(Class<?> type, Set<Class<?>> visited) {
        if (type == null || !visited.add(type)) {
            return Set.of();
        }
        Set<String> markers = new HashSet<>();
        for (Class<?> implementedInterface : type.getInterfaces()) {
            if (RETIRED_DI_MARKERS.contains(implementedInterface.getName())) {
                markers.add(implementedInterface.getName());
            }
            markers.addAll(findRetiredMarkers(implementedInterface, visited));
        }
        markers.addAll(findRetiredMarkers(type.getSuperclass(), visited));
        return Set.copyOf(markers);
    }
}
