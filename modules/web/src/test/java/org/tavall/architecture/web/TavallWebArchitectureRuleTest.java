package org.tavall.architecture.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.architecture.core.ArchitectureContext;
import org.tavall.architecture.core.ArchitectureViolation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TavallWebArchitectureRuleTest {
    @TempDir
    Path temporaryDirectory;

    private String previousClassRoots;
    private String previousSourceRoots;

    @BeforeEach
    void captureArchitectureProperties() {
        previousClassRoots = System.getProperty("tavall.architecture.classRoots");
        previousSourceRoots = System.getProperty("tavall.architecture.sourceRoots");
    }

    @AfterEach
    void restoreArchitectureProperties() {
        restore("tavall.architecture.classRoots", previousClassRoots);
        restore("tavall.architecture.sourceRoots", previousSourceRoots);
    }

    @Test
    void rejectsFrameworkAndApplicationDependenciesInCanonicalApi() throws IOException {
        Path repository = createProject("tavall-web", "tavall-web-api");
        write(repository, "tavall-web-api/src/main/java/org/tavall/web/api/Page.java", """
                package org.tavall.web.api;
                import org.springframework.context.ApplicationContext;
                public interface Page { }
                """);
        write(repository, "tavall-web-api/src/main/java/org/springframework/context/ApplicationContext.java", """
                package org.springframework.context;
                public interface ApplicationContext { }
                """);
        write(repository, "tavall-web-api/build.gradle.kts", """
                plugins { `java-library` }
                dependencies { implementation(project(\":tavall-web-app\")) }
                """);

        Set<String> rules = validate(repository.resolve("tavall-web-api/src/main/java")).stream()
                .map(ArchitectureViolation::ruleId)
                .collect(Collectors.toSet());

        assertTrue(rules.contains("web-api-dependency-direction"));
        assertTrue(rules.contains("web-api-framework-independence"));
    }

    @Test
    void rejectsProductImplementationImportsAndRuntimeDependencies() throws IOException {
        Path repository = createProject("tavall-web-mc", "tavall-web-mc");
        write(repository, "build.gradle.kts", """
                plugins { `java-library` }
                dependencies { api(\"org.tavall:tavall-web-app:0.1.1-SNAPSHOT\") }
                """);
        write(repository, "src/main/java/org/tavall/novus/web/pages/home/HomePage.java", """
                package org.tavall.novus.web.pages.home;
                import org.tavall.novus.web.account.persistence.TavallAccountEntity;
                public final class HomePage { }
                """);
        write(repository, "src/main/java/org/tavall/novus/web/account/persistence/TavallAccountEntity.java", """
                package org.tavall.novus.web.account.persistence;
                public final class TavallAccountEntity { }
                """);

        Set<String> rules = validate(repository.resolve("src/main/java")).stream()
                .map(ArchitectureViolation::ruleId)
                .collect(Collectors.toSet());

        assertTrue(rules.contains("web-product-api-dependency"));
        assertTrue(rules.contains("web-product-runtime-dependency"));
        assertTrue(rules.contains("web-product-implementation-import"));
    }

    @Test
    void rejectsDependencyLookupInFrontendBuildersAndMissingGeneratedSymbols() throws IOException {
        Path repository = createProject("tavall-web", "tavall-web-frontend");
        write(repository, "tavall-web-frontend/build.gradle.kts", """
                plugins { `java-library` }
                dependencies { api(project(\":tavall-web-api\")) }
                """);
        write(repository, "tavall-web-frontend/src/main/java/org/tavall/web/frontend/abstracts/html/AbstractPageHTMLBuilder.java", """
                package org.tavall.web.frontend.abstracts.html;
                import org.tavall.dependency.DependencyAccess;
                public abstract class AbstractPageHTMLBuilder implements DependencyAccess {
                    Object source() { return getInstance(); }
                }
                """);
        write(repository, "tavall-web-frontend/src/main/java/org/tavall/dependency/DependencyAccess.java", """
                package org.tavall.dependency;
                public interface DependencyAccess { default Object getInstance() { return null; } }
                """);
        write(repository, "tavall-web-mc/build.gradle.kts", """
                plugins { `java-library` }
                dependencies { api(\"org.tavall:tavall-web-api:0.1.1-SNAPSHOT\") }
                """);
        write(repository, "tavall-web-mc/src/main/java/org/tavall/novus/web/pages/home/HomePageHTML.java", """
                package org.tavall.novus.web.pages.home;
                public final class HomePageHTML { void build() { HTML.div().classes(\"hero\"); } }
                """);
        write(repository, "tavall-web-mc/src/main/java/org/tavall/novus/web/pages/home/HTML.java", """
                package org.tavall.novus.web.pages.home;
                final class HTML {
                    static Element div() { return new Element(); }
                    static final class Element { Element classes(String value) { return this; } }
                }
                """);

        List<ArchitectureViolation> violations = validate(
                repository.resolve("tavall-web-frontend/src/main/java"),
                repository.resolve("tavall-web-mc/src/main/java")
        );

        assertTrue(violations.stream().anyMatch(value -> value.ruleId().equals("web-builder-runtime-lookup")));
        assertTrue(violations.stream().anyMatch(value -> value.ruleId().equals("web-generated-frontend-symbols")));
    }

    private Path createProject(String rootName, String moduleName) throws IOException {
        Path repository = temporaryDirectory.resolve(rootName + "-" + moduleName);
        Files.createDirectories(repository);
        write(repository, "settings.gradle.kts", "rootProject.name = \"" + rootName + "\"\n");
        write(repository, moduleName + "/build.gradle.kts", "plugins { `java-library` }\n");
        return repository;
    }

    private Path write(Path repository, String relativePath, String content) throws IOException {
        Path file = repository.resolve(relativePath);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content);
    }

    private List<ArchitectureViolation> validate(Path... sourceRoots) throws IOException {
        Path repository = repositoryRoot(sourceRoots[0]);
        Path classRoot = Files.createDirectories(temporaryDirectory.resolve("compiled-consumer"));
        compileProductionSources(repository, classRoot);
        System.setProperty("tavall.architecture.classRoots", classRoot.toAbsolutePath().toString());
        System.setProperty("tavall.architecture.sourceRoots",
                java.util.Arrays.stream(sourceRoots)
                        .map(Path::toAbsolutePath)
                        .map(Path::toString)
                        .collect(Collectors.joining(java.io.File.pathSeparator)));
        return new TavallWebArchitectureRule().validate(ArchitectureContext.fromSystemProperties());
    }

    private static Path repositoryRoot(Path sourceRoot) {
        for (Path path = sourceRoot; path != null; path = path.getParent()) {
            if (Files.isRegularFile(path.resolve("settings.gradle.kts"))
                    || Files.isRegularFile(path.resolve("settings.gradle"))) {
                return path;
            }
        }
        throw new IllegalStateException("No fixture settings file for source root " + sourceRoot);
    }

    private static void compileProductionSources(Path repository, Path output) throws IOException {
        List<Path> sources;
        try (Stream<Path> paths = Files.walk(repository)) {
            sources = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("/src/test/"))
                    .sorted()
                    .toList();
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("A JDK compiler is required for Web architecture rule fixtures");
        }
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(null, null, null)) {
            Iterable<? extends javax.tools.JavaFileObject> units = manager.getJavaFileObjectsFromPaths(sources);
            List<String> options = new ArrayList<>(List.of("-proc:none", "-d", output.toString()));
            Boolean compiled = compiler.getTask(null, manager, null, options, null, units).call();
            if (!Boolean.TRUE.equals(compiled)) {
                throw new IllegalStateException("Web architecture consumer fixture did not compile");
            }
        }
    }

    private static void restore(String property, String previous) {
        if (previous == null) {
            System.clearProperty(property);
        } else {
            System.setProperty(property, previous);
        }
    }
}
