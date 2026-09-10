package org.tavall.architecture.gradle;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TavallArchitectureTestsPluginTest {
    @Test
    void applyingPluginRegistersExecutableArchitectureGate() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(TavallArchitectureTestsPlugin.class);

        assertNotNull(project.getTasks().findByName("architectureTest"));
        assertTrue(project.getTasks().getByName("architectureTest") instanceof org.gradle.api.tasks.testing.Test);
        assertTrue(project.getTasks().getByName("check").getTaskDependencies()
                .getDependencies(project.getTasks().getByName("check"))
                .contains(project.getTasks().getByName("architectureTest")));
    }
}
