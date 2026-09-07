package org.tavall.architecture;

import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.dependency.metadata.interfaces.IDependencyMetaData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class TavallDependencyMapIndex {
    List<Binding> index(DependencyMap dependencyMap) {
        IdentityHashMap<IDependencyMetaData<?, ?>, MutableBinding> bindings =
                new IdentityHashMap<>();

        dependencyMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Class::getName)))
                .forEach(entry -> bindings
                        .computeIfAbsent(
                                entry.getValue(),
                                MutableBinding::new
                        )
                        .tokens
                        .add(entry.getKey()));

        return bindings.values().stream()
                .map(MutableBinding::freeze)
                .sorted(Comparator.comparing(binding ->
                        binding.concreteType().getName()))
                .toList();
    }

    List<TavallArchitectureRuleSet.Violation> audit(Binding binding) {
        List<TavallArchitectureRuleSet.Violation> violations = new ArrayList<>();
        Class<?> concreteType = binding.concreteType();

        if (!binding.tokens().contains(concreteType)) {
            violations.add(new TavallArchitectureRuleSet.Violation(
                    "di-concrete-token",
                    concreteType.getName()
                            + " is not registered under its implicit concrete token"
            ));
        }

        for (Class<?> token : binding.tokens()) {
            if (!token.isAssignableFrom(concreteType)) {
                violations.add(new TavallArchitectureRuleSet.Violation(
                        "di-token-assignability",
                        concreteType.getName()
                                + " cannot satisfy mapped token "
                                + token.getName()
                ));
            }
        }

        DelegatesTo delegatesTo = concreteType.getAnnotation(DelegatesTo.class);
        if (delegatesTo != null) {
            Arrays.stream(delegatesTo.value())
                    .filter(alias -> !binding.tokens().contains(alias))
                    .forEach(alias -> violations.add(
                            new TavallArchitectureRuleSet.Violation(
                                    "di-missing-alias",
                                    concreteType.getName()
                                            + " declares but does not map "
                                            + alias.getName()
                            )
                    ));
        }

        return List.copyOf(violations);
    }

    record Binding(
            IDependencyMetaData<?, ?> metadata,
            Class<?> concreteType,
            Set<Class<?>> tokens
    ) {
        Binding {
            tokens = Set.copyOf(tokens);
        }
    }

    private static final class MutableBinding {
        private final IDependencyMetaData<?, ?> metadata;
        private final Set<Class<?>> tokens = new LinkedHashSet<>();

        private MutableBinding(IDependencyMetaData<?, ?> metadata) {
            this.metadata = metadata;
        }

        private Binding freeze() {
            Class<?> concreteType = metadata.getConcreteType();
            if (concreteType == null) {
                throw new IllegalStateException(
                        "Dependency metadata does not expose a concrete type"
                );
            }
            return new Binding(metadata, concreteType, tokens);
        }
    }
}
