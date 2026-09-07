package org.tavall.architecture;

import org.junit.jupiter.api.Test;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.dependency.metadata.interfaces.IDependencyMetaData;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TavallDependencyMapArchitectureTest {
    @Test
    void groupsConcreteAndInterfaceTokensBySharedMetadataIdentity() {
        DependencyMap dependencyMap = new DependencyMap();
        TestRuntimeHandler handler = new TestRuntimeHandler();

        dependencyMap.registerInstance(TestRuntimeHandler.class, handler);
        IDependencyMetaData<?, ?> metadata =
                dependencyMap.findMetaData(TestRuntimeHandler.class);
        dependencyMap.registerDependency(ITestRuntimeHandler.class, metadata);

        TavallDependencyMapIndex index = new TavallDependencyMapIndex();
        List<TavallDependencyMapIndex.Binding> bindings = index.index(dependencyMap);

        assertEquals(1, bindings.size());
        TavallDependencyMapIndex.Binding binding = bindings.getFirst();
        assertSame(metadata, binding.metadata());
        assertEquals(TestRuntimeHandler.class, binding.concreteType());
        assertEquals(
                Set.of(TestRuntimeHandler.class, ITestRuntimeHandler.class),
                binding.tokens()
        );
        assertTrue(index.audit(binding).isEmpty());
    }

    @Test
    void clearingRuntimeMapRemovesEveryAlias() {
        DependencyMap dependencyMap = new DependencyMap();
        TestRuntimeHandler handler = new TestRuntimeHandler();

        dependencyMap.registerInstance(TestRuntimeHandler.class, handler);
        dependencyMap.registerDependency(
                ITestRuntimeHandler.class,
                dependencyMap.findMetaData(TestRuntimeHandler.class)
        );

        dependencyMap.clear();

        assertTrue(dependencyMap.isDependencyMapEmpty());
        assertEquals(0, dependencyMap.getDependencyMapSize());
    }

    private interface ITestRuntimeHandler {
    }

    @DelegatesTo(ITestRuntimeHandler.class)
    private static final class TestRuntimeHandler
            implements ITestRuntimeHandler {
    }
}
