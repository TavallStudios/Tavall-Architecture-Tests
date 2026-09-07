package org.tavall.architecture;

import org.tavall.dependency.annotations.DelegatesTo;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class TavallArchitectureRuleSet {
    private static final Set<String> RETIRED_DI_MARKERS = Set.of(
            "org.tavall.dependency.IDependencyInjectableConcrete",
            "org.tavall.dependency.IDependencyInjectableInterface"
    );

    List<Violation> audit(
            TavallProductionClassIndex.TavallProductionClass productionClass
    ) {
        List<Violation> violations = new ArrayList<>();

        if (productionClass.origins().size() != 1) {
            violations.add(new Violation(
                    "duplicate-class",
                    productionClass.className()
                            + " is emitted by "
                            + productionClass.origins()
            ));
        }

        if (!productionClass.loaded()) {
            Throwable failure = productionClass.loadFailure();
            violations.add(new Violation(
                    "class-load",
                    productionClass.className()
                            + " could not load without initialization: "
                            + failure.getClass().getName()
                            + ": "
                            + String.valueOf(failure.getMessage())
            ));
            return List.copyOf(violations);
        }

        Class<?> type = productionClass.type();
        if (!type.getName().startsWith("org.tavall.")) {
            violations.add(new Violation(
                    "namespace",
                    type.getName() + " is outside the Tavall namespace"
            ));
        }

        findRetiredMarkers(type, new HashSet<>()).forEach(marker ->
                violations.add(new Violation(
                        "retired-di-marker",
                        type.getName() + " still depends on " + marker
                ))
        );

        DelegatesTo delegatesTo = type.getAnnotation(DelegatesTo.class);
        if (delegatesTo != null) {
            auditDelegation(type, delegatesTo, violations);
        }

        return List.copyOf(violations);
    }

    private void auditDelegation(
            Class<?> concreteType,
            DelegatesTo delegatesTo,
            List<Violation> violations
    ) {
        if (concreteType.isInterface() || Modifier.isAbstract(concreteType.getModifiers())) {
            violations.add(new Violation(
                    "delegation-concrete",
                    concreteType.getName()
                            + " declares @DelegatesTo but is not concrete"
            ));
        }

        Set<Class<?>> uniqueAliases = new HashSet<>();
        Arrays.stream(delegatesTo.value()).forEach(alias -> {
            if (!uniqueAliases.add(alias)) {
                violations.add(new Violation(
                        "delegation-duplicate",
                        concreteType.getName()
                                + " repeats delegation alias "
                                + alias.getName()
                ));
            }
            if (!alias.isAssignableFrom(concreteType)) {
                violations.add(new Violation(
                        "delegation-assignability",
                        concreteType.getName()
                                + " cannot satisfy delegation alias "
                                + alias.getName()
                ));
            }
        });
    }

    private Set<String> findRetiredMarkers(
            Class<?> type,
            Set<Class<?>> visited
    ) {
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

    record Violation(String rule, String message) {
    }
}
