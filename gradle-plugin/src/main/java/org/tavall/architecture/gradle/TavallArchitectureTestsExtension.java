package org.tavall.architecture.gradle;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;

public abstract class TavallArchitectureTestsExtension {
    public abstract ListProperty<String> getModules();

    public abstract RegularFileProperty getDebtFile();
}
