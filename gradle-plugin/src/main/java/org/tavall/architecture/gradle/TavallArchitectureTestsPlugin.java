package org.tavall.architecture.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.repositories.PasswordCredentials;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TavallArchitectureTestsPlugin implements Plugin<Project> {
    private static final String PACKAGES_URL =
            "https://maven.pkg.github.com/TavallStudios/Tavall-Architecture-Tests";
    private static final Set<String> SUPPORTED_MODULES = Set.of(
            "core", "patterns", "di", "registry", "cache", "database", "runtime"
    );

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);
        configureArchitectureRepository(project);

        TavallArchitectureTestsExtension extension = project.getExtensions().create(
                "architectureTests",
                TavallArchitectureTestsExtension.class
        );
        extension.getModules().convention(List.of("core", "patterns"));

        Configuration moduleArtifacts = project.getConfigurations().create(
                "tavallArchitectureTestModules",
                configuration -> {
                    configuration.setCanBeConsumed(false);
                    configuration.setCanBeResolved(true);
                    configuration.setTransitive(false);
                }
        );
        Configuration runtime = project.getConfigurations().create(
                "tavallArchitectureTestRuntime",
                configuration -> {
                    configuration.setCanBeConsumed(false);
                    configuration.setCanBeResolved(true);
                }
        );

        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        TaskProvider<Sync> unpack = project.getTasks().register(
                "unpackTavallArchitectureTests",
                Sync.class,
                task -> {
                    task.setDescription("Materializes the canonical JUnit entrypoint from tavall-architecture-core.");
                    task.setGroup("verification");
                    task.into(project.getLayout().getBuildDirectory().dir("tavall-architecture-tests/classes"));
                    task.from(project.provider(() -> moduleArtifacts.getFiles().stream()
                            .filter(file -> file.getName().startsWith("tavall-architecture-core-"))
                            .map(project::zipTree)
                            .toList()));
                }
        );

        TaskProvider<Test> architectureTest = project.getTasks().register(
                "architectureTest",
                Test.class,
                task -> {
                    task.setDescription("Runs canonical Tavall architecture tests against this consumer.");
                    task.setGroup("verification");
                    task.dependsOn(project.getTasks().named(JavaPlugin.CLASSES_TASK_NAME), unpack);
                    task.setTestClassesDirs(project.files(unpack.map(Sync::getDestinationDir)));
                    task.setClasspath(project.files(
                            unpack.map(Sync::getDestinationDir),
                            runtime,
                            main.getOutput(),
                            main.getRuntimeClasspath()
                    ));
                    task.useJUnitPlatform();
                    task.setMaxParallelForks(1);
                    task.doFirst(ignored -> {
                        task.systemProperty(
                                "tavall.architecture.classRoots",
                                main.getOutput().getClassesDirs().getAsPath()
                        );
                        String sourceRoots = main.getAllJava().getSourceDirectories().getFiles().stream()
                                .map(File::getAbsolutePath)
                                .sorted()
                                .reduce((left, right) -> left + File.pathSeparator + right)
                                .orElse("");
                        task.systemProperty("tavall.architecture.sourceRoots", sourceRoots);
                        if (extension.getDebtFile().isPresent()) {
                            task.systemProperty(
                                    "tavall.architecture.debtFile",
                                    extension.getDebtFile().get().getAsFile().getAbsolutePath()
                            );
                        }
                    });
                }
        );

        project.getTasks().named("check").configure(task -> task.dependsOn(architectureTest));

        project.afterEvaluate(ignored -> {
            String version = architectureVersion(project);
            LinkedHashSet<String> selected = new LinkedHashSet<>();
            selected.add("core");
            for (String requested : extension.getModules().get()) {
                String module = requested.toLowerCase(Locale.ROOT).strip();
                if (!SUPPORTED_MODULES.contains(module)) {
                    throw new IllegalArgumentException("Unknown Tavall architecture-test module: " + requested);
                }
                selected.add(module);
            }
            for (String module : selected) {
                String coordinate = "org.tavall:tavall-architecture-" + module + ":" + version;
                project.getDependencies().add(moduleArtifacts.getName(), coordinate);
                project.getDependencies().add(runtime.getName(), coordinate);
            }
        });
    }

    private static void configureArchitectureRepository(Project project) {
        String token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.isBlank()) {
            return;
        }
        String actor = System.getenv("GITHUB_ACTOR");
        if (actor == null || actor.isBlank()) {
            actor = "github";
        }
        String username = actor;
        project.getRepositories().maven(repository -> {
            repository.setName("TavallArchitectureTests");
            repository.setUrl(project.uri(PACKAGES_URL));
            repository.credentials(PasswordCredentials.class, credentials -> {
                credentials.setUsername(username);
                credentials.setPassword(token);
            });
        });
    }

    private static String architectureVersion(Project project) {
        Object override = project.findProperty("tavallArchitectureVersion");
        if (override != null && !override.toString().isBlank()) {
            return override.toString().strip();
        }
        String version = TavallArchitectureTestsPlugin.class.getPackage().getImplementationVersion();
        if (version == null || version.isBlank()) {
            throw new IllegalStateException(
                    "Tavall architecture plugin has no implementation version; use a published plugin artifact "
                            + "or set tavallArchitectureVersion for an intentional local/composite test."
            );
        }
        return version;
    }
}
