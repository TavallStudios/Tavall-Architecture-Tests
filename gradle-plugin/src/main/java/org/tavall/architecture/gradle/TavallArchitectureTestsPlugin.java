package org.tavall.architecture.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.repositories.PasswordCredentials;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.logging.TestExceptionFormat;
import org.tavall.architecture.core.TestAuthoringPolicy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
            "core", "patterns", "testing", "di", "registry", "cache", "database", "runtime"
    );

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);
        configureArchitectureRepository(project);

        TavallArchitectureTestsExtension extension = project.getExtensions().create(
                "architectureTests",
                TavallArchitectureTestsExtension.class
        );
        extension.getModules().convention(List.of("core", "patterns", "testing"));
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

        TaskProvider<JavaExec> architectureAnalyze = project.getTasks().register(
                "architectureAnalyze",
                JavaExec.class,
                task -> {
                    task.setDescription("Analyzes production classes and test authoring with per-class Tavall architecture results.");
                    task.setGroup("verification");
                    task.getMainClass().set("org.tavall.architecture.core.ArchitectureAnalysisMain");
                }
        );

        TaskProvider<DefaultTask> generateTestScaffold = project.getTasks().register(
                "generateTavallTestScaffold",
                DefaultTask.class,
                task -> {
                    task.setDescription("Creates a fail-closed JUnit 5 scaffold for -PtavallTestClass=<production FQCN>.");
                    task.setGroup("verification");
                }
        );

        project.getTasks().named("check").configure(task -> task.dependsOn(architectureTest));
        project.afterEvaluate(ignored -> configureRuleModules(project, extension, moduleArtifacts, runtime));

        project.getGradle().projectsEvaluated(ignored -> {
            List<ArchitectureTarget> targets = architectureTargets(project, extension);
            architectureTest.configure(task -> configureArchitectureTest(
                    project,
                    extension,
                    runtime,
                    unpack,
                    task,
                    targets
            ));
            architectureAnalyze.configure(task -> configureArchitectureAnalysis(
                    project,
                    extension,
                    runtime,
                    task,
                    targets
            ));
            generateTestScaffold.configure(task -> task.doLast(unused -> generateTestScaffold(project, targets)));
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

    private static void configureArchitectureTest(
            Project project,
            TavallArchitectureTestsExtension extension,
            Configuration runtime,
            TaskProvider<Sync> unpack,
            Test task,
            List<ArchitectureTarget> targets
    ) {
        task.dependsOn(targetClassTasks(targets));
        task.setClasspath(project.files(
                unpack.map(Sync::getDestinationDir),
                runtime,
                targetClasspath(targets)
        ));
        task.doFirst(ignored -> task.systemProperties(architectureSystemProperties(project, extension, targets)));
    }

    private static void configureArchitectureAnalysis(
            Project project,
            TavallArchitectureTestsExtension extension,
            Configuration runtime,
            JavaExec task,
            List<ArchitectureTarget> targets
    ) {
        task.dependsOn(targetClassTasks(targets));
        task.setClasspath(project.files(runtime, targetClasspath(targets)));
        task.doFirst(ignored -> task.systemProperties(architectureSystemProperties(project, extension, targets)));
    }

    private static List<Object> targetClasspath(List<ArchitectureTarget> targets) {
        List<Object> targetClasspath = new ArrayList<>();
        for (ArchitectureTarget target : targets) {
            targetClasspath.add(target.main().getOutput());
            targetClasspath.add(target.main().getRuntimeClasspath());
        }
        return List.copyOf(targetClasspath);
    }

    private static List<?> targetClassTasks(List<ArchitectureTarget> targets) {
        return targets.stream()
                .map(target -> target.project().getTasks().named(JavaPlugin.CLASSES_TASK_NAME))
                .toList();
    }

    private static Map<String, Object> architectureSystemProperties(
            Project project,
            TavallArchitectureTestsExtension extension,
            List<ArchitectureTarget> targets
    ) {
        String classRoots = joinExistingDirectories(targets.stream()
                .flatMap(target -> target.main().getOutput().getClassesDirs().getFiles().stream())
                .toList());
        if (classRoots.isBlank()) {
            throw new IllegalStateException("No compiled Tavall production class roots were found for architecture targets");
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("tavall.architecture.classRoots", classRoots);
        properties.put("tavall.architecture.sourceRoots", joinExistingDirectories(targets.stream()
                .flatMap(target -> target.main().getAllJava().getSourceDirectories().getFiles().stream())
                .toList()));
        properties.put("tavall.architecture.testSourceRoots", joinExistingDirectories(testSourceDirectories(project, targets)));
        properties.put(
                "tavall.architecture.targetProjects",
                targets.stream().map(target -> target.project().getPath()).sorted().reduce(
                        (left, right) -> left + "," + right
                ).orElse("")
        );
        properties.put(
                "tavall.architecture.reportFile",
                project.getLayout().getBuildDirectory()
                        .file("reports/tavall-architecture/architecture-report.json")
                        .get().getAsFile().getAbsolutePath()
        );
        if (extension.getDebtFile().isPresent()) {
            properties.put(
                    "tavall.architecture.debtFile",
                    extension.getDebtFile().get().getAsFile().getAbsolutePath()
            );
        }
        return Map.copyOf(properties);
    }

    private static List<File> testSourceDirectories(Project applyingProject, List<ArchitectureTarget> targets) {
        LinkedHashSet<File> roots = new LinkedHashSet<>();
        SourceSetContainer applyingSourceSets = applyingProject.getExtensions().getByType(SourceSetContainer.class);
        roots.addAll(applyingSourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)
                .getAllJava().getSourceDirectories().getFiles());
        for (ArchitectureTarget target : targets) {
            roots.addAll(target.test().getAllJava().getSourceDirectories().getFiles());
        }
        return List.copyOf(roots);
    }

    private static void generateTestScaffold(Project applyingProject, List<ArchitectureTarget> targets) {
        Object configured = applyingProject.findProperty("tavallTestClass");
        if (configured == null || configured.toString().isBlank()) {
            throw new IllegalArgumentException(
                    "generateTavallTestScaffold requires -PtavallTestClass=<org.tavall.production.Type>"
            );
        }
        String className = configured.toString().strip();
        if (!className.startsWith("org.tavall.")) {
            throw new IllegalArgumentException("Tavall test scaffold target must be an org.tavall production class: " + className);
        }
        String topLevelClass = className.contains("$") ? className.substring(0, className.indexOf('$')) : className;
        String productionRelativePath = topLevelClass.replace('.', File.separatorChar) + ".java";
        List<ArchitectureTarget> owners = targets.stream().filter(target ->
                target.main().getAllJava().getSourceDirectories().getFiles().stream()
                        .map(root -> new File(root, productionRelativePath))
                        .anyMatch(File::isFile)
        ).toList();
        if (owners.size() != 1) {
            throw new IllegalStateException(
                    "Expected exactly one architecture target to own " + className + " but found " + owners.size()
            );
        }
        ArchitectureTarget owner = owners.getFirst();
        File testRoot = preferredTestSourceRoot(owner);
        Path output = testRoot.toPath().resolve(TestAuthoringPolicy.expectedTestRelativePath(className));
        if (Files.exists(output)) {
            throw new IllegalStateException("Test source already exists: " + output);
        }
        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, TestAuthoringPolicy.renderScaffold(className));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create Tavall test scaffold " + output, exception);
        }
        applyingProject.getLogger().lifecycle("Created fail-closed Tavall test scaffold: {}", output);
    }

    private static File preferredTestSourceRoot(ArchitectureTarget target) {
        File conventional = new File(target.project().getProjectDir(), "src/test/java").getAbsoluteFile();
        List<File> configured = target.test().getAllJava().getSourceDirectories().getFiles().stream()
                .map(File::getAbsoluteFile)
                .sorted(java.util.Comparator.comparing(File::getAbsolutePath))
                .toList();
        if (configured.contains(conventional) || configured.isEmpty()) {
            return conventional;
        }
        return configured.getFirst();
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
                    sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME),
                    sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)
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

    private record ArchitectureTarget(Project project, SourceSet main, SourceSet test) {
    }
}
