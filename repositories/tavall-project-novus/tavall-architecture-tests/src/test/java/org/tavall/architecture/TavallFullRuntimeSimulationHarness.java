package org.tavall.architecture;

import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.dependency.maps.DependencyMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TavallFullRuntimeSimulationHarness {
    private final TavallProductionClassIndex classIndex =
            new TavallProductionClassIndex();
    private final TavallArchitectureRuleSet ruleSet =
            new TavallArchitectureRuleSet();
    private final TavallDependencyMapIndex dependencyMapIndex =
            new TavallDependencyMapIndex();

    void simulate(RuntimeDefinition definition) {
        List<TavallProductionClassIndex.TavallProductionClass> allClasses =
                classIndex.discover();
        List<TavallProductionClassIndex.TavallProductionClass> runtimeClasses =
                allClasses.stream()
                        .filter(productionClass -> definition.packagePrefixes()
                                .stream()
                                .anyMatch(productionClass.className()::startsWith))
                        .toList();

        assertFalse(
                runtimeClasses.isEmpty(),
                definition.runtimeId() + " discovered no runtime classes"
        );

        List<TavallArchitectureRuleSet.Violation> violations = new ArrayList<>();
        runtimeClasses.forEach(productionClass ->
                violations.addAll(ruleSet.audit(productionClass))
        );

        for (String entrypoint : definition.entrypointClassNames()) {
            TavallProductionClassIndex.TavallProductionClass entrypointClass =
                    allClasses.stream()
                            .filter(candidate -> candidate.className().equals(entrypoint))
                            .findFirst()
                            .orElseThrow(() -> new AssertionError(
                                    definition.runtimeId()
                                            + " is missing entrypoint "
                                            + entrypoint
                            ));
            assertTrue(
                    entrypointClass.loaded(),
                    () -> entrypoint + " failed to load without initialization"
            );
            assertNotNull(entrypointClass.type());
        }

        DependencyMap dependencyMap = new DependencyMap();
        SimulationRuntimeHandler runtimeHandler =
                new SimulationRuntimeHandler(definition.runtimeId());
        dependencyMap.registerInstance(
                SimulationRuntimeHandler.class,
                runtimeHandler
        );
        dependencyMap.registerDependency(
                ISimulationRuntimeHandler.class,
                dependencyMap.findMetaData(SimulationRuntimeHandler.class)
        );

        dependencyMapIndex.index(dependencyMap).forEach(binding ->
                violations.addAll(dependencyMapIndex.audit(binding))
        );

        dependencyMap.clear();
        assertTrue(
                dependencyMap.isDependencyMapEmpty(),
                definition.runtimeId() + " left DI aliases after shutdown"
        );
        assertTrue(
                violations.isEmpty(),
                () -> formatViolations(definition, violations)
        );
    }

    private String formatViolations(
            RuntimeDefinition definition,
            List<TavallArchitectureRuleSet.Violation> violations
    ) {
        return violations.stream()
                .map(violation -> definition.runtimeId()
                        + ": "
                        + violation.rule()
                        + ": "
                        + violation.message())
                .collect(java.util.stream.Collectors.joining(System.lineSeparator()));
    }

    record RuntimeDefinition(
            String runtimeId,
            Set<String> entrypointClassNames,
            Set<String> packagePrefixes
    ) {
        RuntimeDefinition {
            entrypointClassNames = Set.copyOf(entrypointClassNames);
            packagePrefixes = Set.copyOf(packagePrefixes);
        }
    }

    private interface ISimulationRuntimeHandler {
        String runtimeId();
    }

    @DelegatesTo(ISimulationRuntimeHandler.class)
    private static final class SimulationRuntimeHandler
            implements ISimulationRuntimeHandler {
        private final String runtimeId;

        private SimulationRuntimeHandler(String runtimeId) {
            this.runtimeId = runtimeId;
        }

        @Override
        public String runtimeId() {
            return runtimeId;
        }
    }
}
