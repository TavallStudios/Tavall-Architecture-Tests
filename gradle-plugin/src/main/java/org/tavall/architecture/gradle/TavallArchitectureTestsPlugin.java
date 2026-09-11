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
import org.gradle.api.tasks.testing.logging.TestExceptionFormat;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        extension.getTargetProjects().convention(List.of());

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
                    task.setDescription("Runs canonical Tavall architecture tests against configured production targets.");
                    task.setGroup("verification");
                    task.dependsOn(unpack);
                    task.setTestClassesDirs(project.files(unpack.map(Sync::getDestinationDir)));
                    task.useJUnitPlatform();
                    task.setMaxParallelForks(1);
                    task.getTestLogging().setExceptionFormat(TestExceptionFormat.FULL);
                    task.getTestLogging().setShowCauses(true);
                    task.getTestLogging().setShowExceptions(true);
                    task.getTestLogging().setShowStackTraces(true);
                }
        );

        project.getTasks().named("check").configure(task -> task.dependsOn(architectureTest));

        project.afterEvaluate(ignored -> configureRuleModules(project, extension, moduleArtifacts, runtime));

        project.getGradle().projectsEvaluated(ignored -> {
            List<ArchitectureTarget> targets = architectureTargets(project, extension);
            architectureTest.configure(task -> configureArchitectureTask(
                    project,
                    extension,
                    runtime,
                    unpack,
                    task,
                    targets
            ));
        });
    }

    private static void configureRuleModules(
            Project project,
            TavallArchitectureTestsExtension extension,
            Configuration moduleArtifacts,
            Configuration runtime
    ) {
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
    }

    private static void configureArchitectureTask(
            Project project,
            TavallArchitectureTestsExtension extension,
            Configuration runtime,
            TaskProvider<Sync> unpack,
            Test task,
            List<ArchitectureTarget> targets
    ) {
        task.dependsOn(targets.stream()
                .map(target -> target.project().getTasks().named(JavaPlugin.CLASSES_TASK_NAME))
                .toList());

        List<Object> targetClasspath = new ArrayList<>();
        for (ArchitectureTarget target : targets) {
            targetClasspath.add(target.main().getOutput());
            targetClasspath.add(target.main().getRuntimeClasspath());
        }
        task.setClasspath(project.files(
                unpack.map(Sync::getDestinationDir),
                runtime,
                targetClasspath
        ));

        task.doFirst(ignored -> {
            String classRoots = joinExistingDirectories(targets.stream()
                    .flatMap(target -> target.main().getOutput().getClassesDirs().getFiles().stream())
                    .toList());
            if (classRoots.isBlank()) {
                throw new IllegalStateException("No compiled Tavall production class roots were found for architecture targets");
            }
            task.systemProperty("tavall.architecture.classRoots", classRoots);

            String sourceRoots = joinExistingDirectories(targets.stream()
                    .flatMap(target -> target.main().getAllJava().getSourceDirectories().getFiles().stream())
                    .toList());
            task.systemProperty("tavall.architecture.sourceRoots", sourceRoots);
            task.systemProperty(
                    "tavall.architecture.targetProjects",
                    targets.stream().map(target -> target.project().getPath()).sorted().reduce(
                            (left, right) -> left + "," + right
                    ).orElse("")
            );

            if (extension.getDebtFile().isPresent()) {
                task.systemProperty(
                        "tavall.architecture.debtFile",
                        extension.getDebtFile().get().getAsFile().getAbsolutePath()
                );
            }
        });
    }

    private static List<ArchitectureTarget> architectureTargets(
            Project applyingProject,
            TavallArchitectureTestsExtension extension
    ) {
        List<String> configured = extension.getTargetProjects().get();
        List<Project> projects;
        if (configured.isEmpty()) {
            projects = List.of(applyingProject);
        } else {
            Map<String, Project> resolved = new LinkedHashMap<>();
            for (String rawPath : configured) {
                String requested = rawPath == null ? "" : rawPath.strip();
                if (requested.isEmpty()) {
                    throw new IllegalArgumentException("Architecture target project path must not be blank");
                }
                String path = requested.equals(":") || requested.startsWith(":")
                        ? requested
                        : ":" + requested;
                Project target = applyingProject.getRootProject().findProject(path);
                if (target == null) {
                    throw new IllegalArgumentException("Unknown Tavall architecture target project: " + rawPath);
                }
                resolved.putIfAbsent(target.getPath(), target);
            }
            projects = List.copyOf(resolved.values());
        }

        return projects.stream().map(target -> {
            SourceSetContainer sourceSets = target.getExtensions().findByType(SourceSetContainer.class);
            if (sourceSets == null) {
                throw new IllegalArgumentException(
                        "Tavall architecture target does not expose Java source sets: " + target.getPath()
                );
            }
            return new ArchitectureTarget(
                    target,
                    sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            );
        }).toList();
    }

    private static String joinExistingDirectories(List<File> directories) {
        return directories.stream()
                .filter(File::isDirectory)
                .map(File::getAbsolutePath)
                .distinct()
                .sorted()
                .reduce((left, right) -> left + File.pathSeparator + right)
                .orElse("");
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

    private record ArchitectureTarget(Project project, SourceSet main) {
    }
}
