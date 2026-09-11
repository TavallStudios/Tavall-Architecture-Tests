package org.tavall.architecture.gradle;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;

public abstract class TavallArchitectureTestsExtension {
    public abstract ListProperty<String> getModules();

    /**
     * Gradle project paths whose production main source sets are inspected by the canonical architecture gate.
     * When empty, the plugin preserves the single-project behavior and targets the project that applies it.
     */
    public abstract ListProperty<String> getTargetProjects();

    public abstract RegularFileProperty getDebtFile();
}
