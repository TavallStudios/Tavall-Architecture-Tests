package org.tavall.architecture.gradle;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TavallArchitectureTestsPluginTest {
    @Test
    void applyingPluginRegistersExecutableArchitectureGate() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(TavallArchitectureTestsPlugin.class);

        assertNotNull(project.getTasks().findByName("architectureTest"));
        assertTrue(project.getTasks().getByName("architectureTest") instanceof org.gradle.api.tasks.testing.Test);
        assertNotNull(project.getTasks().findByName("architectureAnalyze"));
        assertTrue(project.getTasks().getByName("architectureAnalyze") instanceof org.gradle.api.tasks.JavaExec);
        assertNotNull(project.getTasks().findByName("generateTavallTestScaffold"));
        assertTrue(project.getTasks().getByName("check").getTaskDependencies()
                .getDependencies(project.getTasks().getByName("check"))
                .contains(project.getTasks().getByName("architectureTest")));
    }

    @Test
    void consumerCheckExecutesCanonicalRuleAndThenPassesWhenFixed(@TempDir Path projectDirectory)
            throws IOException {
        String version = architectureVersion();

        Files.writeString(projectDirectory.resolve("settings.gradle.kts"), "rootProject.name = \"architecture-consumer-smoke\"\n");
        Files.writeString(
                projectDirectory.resolve("build.gradle.kts"),
                """
                plugins {
                    java
                    id("org.tavall.architecture-tests")
                }

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                architectureTests {
                    modules.set(listOf("core", "patterns"))
                }
                """
        );
        Path packageDirectory = projectDirectory.resolve("src/main/java/org/tavall/demo");
        Files.createDirectories(packageDirectory);
        Path violatingSource = packageDirectory.resolve("LegacyRepository.java");
        Files.writeString(violatingSource, "package org.tavall.demo; public final class LegacyRepository {}\n");

        BuildResult rejected = runner(projectDirectory)
                .withArguments("clean", "check", "--stacktrace", "-PtavallArchitectureVersion=" + version)
                .buildAndFail();
        assertNotNull(rejected.task(":architectureTest"));
        assertEquals(TaskOutcome.FAILED, rejected.task(":architectureTest").getOutcome());
        Path resultFile = projectDirectory.resolve(
                "build/test-results/architectureTest/TEST-org.tavall.architecture.core.CanonicalArchitectureTest.xml"
        );
        assertTrue(Files.isRegularFile(resultFile));
        assertTrue(Files.readString(resultFile).contains("repository-type|org.tavall.demo.LegacyRepository"));

        Files.delete(violatingSource);
        Files.writeString(
                packageDirectory.resolve("PlayerService.java"),
                """
                package org.tavall.demo;
                public final class PlayerService {
                    public static void main(String[] args) {
                        Runtime.getRuntime().addShutdownHook(new Thread(() -> {}, "player-service-shutdown"));
                    }
                }
                """
        );
        BuildResult accepted = runner(projectDirectory)
                .withArguments("clean", "check", "--stacktrace", "-PtavallArchitectureVersion=" + version)
                .build();
        assertEquals(TaskOutcome.SUCCESS, accepted.task(":architectureTest").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, accepted.task(":check").getOutcome());
    }

    @Test
    void repositoryTestSuiteInspectsMultipleProductionProjects(@TempDir Path projectDirectory)
            throws IOException {
        String version = architectureVersion();
        Files.writeString(
                projectDirectory.resolve("settings.gradle.kts"),
                """
                rootProject.name = "architecture-suite-boundary-smoke"
                include(":service-a", ":service-b", ":test-suite")
                """
        );
        Files.writeString(projectDirectory.resolve("build.gradle.kts"), "plugins { base }\n");
        Path serviceA = projectDirectory.resolve("service-a");
        Path serviceB = projectDirectory.resolve("service-b");
        Path testSuite = projectDirectory.resolve("test-suite");
        Files.createDirectories(serviceA);
        Files.createDirectories(serviceB);
        Files.createDirectories(testSuite);
        Files.writeString(serviceA.resolve("build.gradle.kts"), "plugins { java }\n");
        Files.writeString(serviceB.resolve("build.gradle.kts"), "plugins { java }\n");
        Files.writeString(
                testSuite.resolve("build.gradle.kts"),
                """
                plugins {
                    java
                    id("org.tavall.architecture-tests")
                }

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                architectureTests {
                    modules.set(listOf("core", "patterns"))
                    targetProjects.set(listOf(":service-a", ":service-b"))
                }

                rootProject.tasks.named("check") {
                    dependsOn(tasks.named("check"))
                }
                """
        );
        Path violatingPackage = serviceA.resolve("src/main/java/org/tavall/demo/a");
        Path validPackage = serviceB.resolve("src/main/java/org/tavall/demo/b");
        Files.createDirectories(violatingPackage);
        Files.createDirectories(validPackage);
        Path violatingSource = violatingPackage.resolve("LegacyRepository.java");
        Files.writeString(violatingSource, "package org.tavall.demo.a; public final class LegacyRepository {}\n");
        Files.writeString(validPackage.resolve("PlayerService.java"), "package org.tavall.demo.b; public final class PlayerService {}\n");

        BuildResult rejected = runner(projectDirectory)
                .withArguments("clean", "check", "--stacktrace", "-PtavallArchitectureVersion=" + version)
                .buildAndFail();
        assertEquals(TaskOutcome.FAILED, rejected.task(":test-suite:architectureTest").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, rejected.task(":service-a:classes").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, rejected.task(":service-b:classes").getOutcome());
        Path resultFile = testSuite.resolve(
                "build/test-results/architectureTest/TEST-org.tavall.architecture.core.CanonicalArchitectureTest.xml"
        );
        assertTrue(Files.readString(resultFile).contains("repository-type|org.tavall.demo.a.LegacyRepository"));

        Files.delete(violatingSource);
        Files.writeString(violatingPackage.resolve("InventoryService.java"), "package org.tavall.demo.a; public final class InventoryService {}\n");
        BuildResult accepted = runner(projectDirectory)
                .withArguments("clean", "check", "--stacktrace", "-PtavallArchitectureVersion=" + version)
                .build();
        assertEquals(TaskOutcome.SUCCESS, accepted.task(":test-suite:architectureTest").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, accepted.task(":test-suite:check").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, accepted.task(":check").getOutcome());
    }

    @Test
    void analysisGuidesTestAuthoringAndProducesPerClassLineEvidence(@TempDir Path projectDirectory)
            throws IOException {
        String version = architectureVersion();
        Files.writeString(projectDirectory.resolve("settings.gradle.kts"), "rootProject.name = \"authoring-smoke\"\n");
        Files.writeString(
                projectDirectory.resolve("build.gradle.kts"),
                """
                plugins {
                    java
                    id("org.tavall.architecture-tests")
                }

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                architectureTests {
                    modules.set(listOf("core", "testing"))
                }
                """
        );
        Path packageDirectory = projectDirectory.resolve("src/main/java/org/tavall/demo");
        Files.createDirectories(packageDirectory);
        Files.writeString(
                packageDirectory.resolve("GreetingService.java"),
                """
                package org.tavall.demo;
                public final class GreetingService {
                    public String greet(String name) {
                        return "Hello " + name;
                    }
                }
                """
        );

        BuildResult missingTest = runner(projectDirectory)
                .withArguments("architectureAnalyze", "--stacktrace", "-PtavallArchitectureVersion=" + version)
                .buildAndFail();
        assertEquals(TaskOutcome.FAILED, missingTest.task(":architectureAnalyze").getOutcome());
        Path reportFile = projectDirectory.resolve("build/reports/tavall-architecture/architecture-report.json");
        String missingReport = Files.readString(reportFile);
        assertTrue(missingReport.contains("\"className\":\"org.tavall.demo.GreetingService\""), missingReport);
        assertTrue(missingReport.contains("\"ruleId\":\"missing-direct-test\""), missingReport);
        assertTrue(missingReport.contains("\"startLine\":"), missingReport);

        BuildResult scaffold = runner(projectDirectory)
                .withArguments(
                        "generateTavallTestScaffold",
                        "-PtavallTestClass=org.tavall.demo.GreetingService",
                        "-PtavallArchitectureVersion=" + version
                )
                .build();
        assertEquals(TaskOutcome.SUCCESS, scaffold.task(":generateTavallTestScaffold").getOutcome());
        Path testSource = projectDirectory.resolve("src/test/java/org/tavall/demo/GreetingServiceTest.java");
        assertTrue(Files.readString(testSource).contains("generatedScaffoldRequiresBehaviorCoverage"));

        BuildResult incompleteTest = runner(projectDirectory)
                .withArguments("architectureAnalyze", "--stacktrace", "-PtavallArchitectureVersion=" + version)
                .buildAndFail();
        String incompleteReport = Files.readString(reportFile);
        assertTrue(incompleteReport.contains("\"ruleId\":\"generated-test-incomplete\""), incompleteReport);

        Files.writeString(
                testSource,
                """
                package org.tavall.demo;

                import org.junit.jupiter.api.Test;

                import static org.junit.jupiter.api.Assertions.assertEquals;

                final class GreetingServiceTest {
                    @Test
                    void greetsNamedPlayer() {
                        GreetingService service = new GreetingService();
                        assertEquals("Hello Ada", service.greet("Ada"));
                    }
                }
                """
        );
        BuildResult accepted = runner(projectDirectory)
                .withArguments("architectureAnalyze", "--stacktrace", "-PtavallArchitectureVersion=" + version)
                .build();
        assertEquals(TaskOutcome.SUCCESS, accepted.task(":architectureAnalyze").getOutcome());
        String acceptedReport = Files.readString(reportFile);
        assertTrue(acceptedReport.contains("\"status\":\"PASS\""), acceptedReport);
        assertTrue(acceptedReport.contains("\"score\":100"), acceptedReport);
    }

    private static String architectureVersion() {
        String version = System.getProperty("tavall.architecture.testVersion");
        assertNotNull(version, "The functional test requires the local architecture module version");
        return version;
    }

    private static GradleRunner runner(Path projectDirectory) {
        return GradleRunner.create()
                .withProjectDir(projectDirectory.toFile())
                .withPluginClasspath()
                .forwardOutput();
    }
}
