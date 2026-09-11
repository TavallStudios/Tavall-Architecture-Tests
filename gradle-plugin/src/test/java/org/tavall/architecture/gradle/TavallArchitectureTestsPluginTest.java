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
        assertTrue(project.getTasks().getByName("check").getTaskDependencies()
                .getDependencies(project.getTasks().getByName("check"))
                .contains(project.getTasks().getByName("architectureTest")));
    }

    @Test
    void consumerCheckExecutesCanonicalRuleAndThenPassesWhenFixed(@TempDir Path projectDirectory)
            throws IOException {
        String version = System.getProperty("tavall.architecture.testVersion");
        assertNotNull(version, "The functional test requires the local architecture module version");

        Files.writeString(
                projectDirectory.resolve("settings.gradle.kts"),
                "rootProject.name = \"architecture-consumer-smoke\"\n"
        );
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
        Files.writeString(
                violatingSource,
                "package org.tavall.demo; public final class LegacyRepository {}\n"
        );

        BuildResult rejected = runner(projectDirectory)
                .withArguments("clean", "check", "--stacktrace", "-PtavallArchitectureVersion=" + version)
                .buildAndFail();
        assertNotNull(rejected.task(":architectureTest"));
        assertEquals(TaskOutcome.FAILED, rejected.task(":architectureTest").getOutcome());
        Path resultFile = projectDirectory.resolve(
                "build/test-results/architectureTest/TEST-org.tavall.architecture.core.CanonicalArchitectureTest.xml"
        );
        assertTrue(Files.isRegularFile(resultFile), "architectureTest must emit a JUnit result file");
        String rejectedResult = Files.readString(resultFile);
        assertTrue(
                rejectedResult.contains("repository-type|org.tavall.demo.LegacyRepository"),
                rejectedResult
        );

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
        assertNotNull(accepted.task(":architectureTest"));
        assertEquals(TaskOutcome.SUCCESS, accepted.task(":architectureTest").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, accepted.task(":check").getOutcome());
    }

    private static GradleRunner runner(Path projectDirectory) {
        return GradleRunner.create()
                .withProjectDir(projectDirectory.toFile())
                .withPluginClasspath()
                .forwardOutput();
    }
}
