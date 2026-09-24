package org.tavall.architecture.web;

import org.tavall.architecture.core.ArchitectureContext;
import org.tavall.architecture.core.ArchitectureRule;
import org.tavall.architecture.core.ArchitectureViolation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class TavallWebArchitectureRule implements ArchitectureRule {
    private static final Pattern ROOT_PROJECT_NAME = Pattern.compile(
            "(?m)^\\s*rootProject\\.name\\s*=\\s*[\"']([^\"']+)[\"']"
    );
    private static final Set<String> PLATFORM_MODULES = Set.of(
            "tavall-web-api",
            "tavall-web-frontend",
            "tavall-web-frontend-codegen",
            "tavall-web-frontend-gradle",
            "tavall-web-app",
            "tavall-web-test-suite"
    );
    private static final List<String> FORBIDDEN_PRODUCT_IMPORTS = List.of(
            "org.tavall.novus.web.account.persistence",
            "org.tavall.novus.web.config",
            "org.tavall.novus.web.content",
            "org.tavall.novus.web.controller",
            "org.tavall.novus.web.platform",
            "org.tavall.novus.web.store"
    );
    private static final List<String> BUILDER_LOOKUP_MARKERS = List.of(
            "org.tavall.dependency.DependencyAccess",
            "org.tavall.dependency.annotations.DelegatesTo",
            "@Autowired",
            "ApplicationContext",
            "getInstance(",
            "EntityManager",
            "JdbcTemplate",
            "Repository"
    );

    @Override
    public String id() {
        return "tavall-web-boundaries";
    }

    @Override
    public List<ArchitectureViolation> validate(ArchitectureContext context) {
        List<ArchitectureViolation> violations = new ArrayList<>();
        for (Path sourceRoot : context.sourceRoots()) {
            if (!Files.isDirectory(sourceRoot)) {
                continue;
            }
            ProjectLocation location = locate(sourceRoot);
            if (location == null) {
                continue;
            }
            inspectProject(location, sourceRoot, violations);
            scanSources(location, sourceRoot, violations);
        }
        return List.copyOf(violations);
    }

    private static void inspectProject(
            ProjectLocation location,
            Path sourceRoot,
            List<ArchitectureViolation> violations
    ) {
        String build = readBuild(location.moduleDirectory());
        if (location.moduleName().equals("tavall-web-api")) {
            if (build.contains("tavall-web-app") || build.contains("org.springframework")
                    || build.contains("spring-boot") || build.contains("thymeleaf")) {
                add(violations, "web-api-dependency-direction", location.moduleName(),
                        "tavall-web-api must remain Spring-independent and must not depend on the executable app");
            }
        }

        if (location.moduleName().equals("tavall-web-app")
                || location.moduleName().equals("tavall-web-frontend")) {
            if (!build.contains("tavall-web-api")) {
                add(violations, "web-platform-api-dependency", location.moduleName(),
                        "Reusable Web platform modules must depend on the canonical tavall-web-api");
            }
        }

        if (isProductModule(location.moduleName())) {
            if (!build.contains("tavall-web-api")) {
                add(violations, "web-product-api-dependency", location.moduleName(),
                        "Java Web product modules must declare a dependency on tavall-web-api");
            }
            if (build.contains("tavall-web-app")) {
                add(violations, "web-product-runtime-dependency", location.moduleName(),
                        "Product modules must not depend on the executable Web application");
            }
        }

        if (location.repositoryName().equals("tavall-mc")) {
            inspectMinecraftOwnership(location, sourceRoot, violations);
        }
    }

    private static void inspectMinecraftOwnership(
            ProjectLocation location,
            Path sourceRoot,
            List<ArchitectureViolation> violations
    ) {
        for (Path path : javaSources(sourceRoot)) {
            String source = read(path);
            String subject = subject(location, path);
            if (importsAny(source, List.of("org.springframework", "org.thymeleaf", "jakarta.servlet"))
                    || source.contains("org.tavall.novus.web")) {
                add(violations, "minecraft-web-ownership", subject,
                        "tavall-mc cannot regain browser, Spring, Thymeleaf, servlet, or Tavall Web implementation ownership");
            }
        }
        for (Path module = location.moduleDirectory(); module != null; module = module.getParent()) {
            if (hasBrowserResources(module)) {
                add(violations, "minecraft-web-resources", subject(location, module),
                        "tavall-mc cannot own browser templates or frontend source trees");
            }
            if (module.equals(location.repositoryRoot())) {
                break;
            }
        }
    }

    private static boolean hasBrowserResources(Path root) {
        return Files.isDirectory(root.resolve("src/main/resources/templates"))
                || Files.isDirectory(root.resolve("src/main/frontend"))
                || Files.isDirectory(root.resolve("src/main/css"))
                || Files.isDirectory(root.resolve("src/main/html"))
                || Files.isDirectory(root.resolve("src/main/ts"));
    }

    private static void scanSources(
            ProjectLocation location,
            Path sourceRoot,
            List<ArchitectureViolation> violations
    ) {
        for (Path path : javaSources(sourceRoot)) {
            String source = read(path);
            String subject = subject(location, path);
            String packageName = packageName(source);

            if (!location.moduleName().equals("tavall-web-api")
                    && packageName.startsWith("org.tavall.web.api")) {
                add(violations, "web-api-duplicate-contract", subject,
                        "Canonical API packages may be declared only by tavall-web-api");
            }

            if (location.moduleName().equals("tavall-web-api")
                    && (importsAny(source, List.of("org.springframework", "jakarta.servlet", "org.thymeleaf"))
                    || source.contains("org.tavall.novus.web"))) {
                add(violations, "web-api-framework-independence", subject,
                        "tavall-web-api contracts must not depend on Spring, Thymeleaf, servlet, or app implementation types");
            }

            if (isProductModule(location.moduleName()) && importsAny(source, FORBIDDEN_PRODUCT_IMPORTS)) {
                add(violations, "web-product-implementation-import", subject,
                        "Web product modules must consume stable API contracts instead of app implementation packages");
            }

            if (location.moduleName().equals("tavall-web-frontend")
                    && isBuilderSource(path, source)
                    && containsAny(source, BUILDER_LOOKUP_MARKERS)) {
                add(violations, "web-builder-runtime-lookup", subject,
                        "Frontend builders must remain deterministic construction code without service or dependency lookup");
            }

            if (isPageCompositionSource(path, source)
                    && containsAny(source, List.of(
                    "org.tavall.dependency.DependencyAccess",
                    "org.tavall.dependency.annotations.DelegatesTo",
                    "@Autowired",
                    "getInstance(",
                    "EntityManager",
                    "JdbcTemplate"
            ))) {
                add(violations, "web-page-data-boundary", subject,
                        "Page composition and builders must receive runtime data from handler/service boundaries");
            }

            if (requiresGeneratedHTMLSymbols(path, source)
                    && !source.contains(".generated.")) {
                add(violations, "web-generated-frontend-symbols", subject,
                        "Structural page CSS/HTML identifiers must use generated frontend symbols");
            }
        }
    }

    private static boolean isProductModule(String moduleName) {
        return moduleName.startsWith("tavall-web-") && !PLATFORM_MODULES.contains(moduleName);
    }

    private static boolean isBuilderSource(Path path, String source) {
        String normalized = path.toString().replace('\\', '/');
        return normalized.contains("/abstracts/")
                || path.getFileName().toString().equals("PageBuilder.java")
                || packageName(source).equals("org.tavall.web.frontend.page");
    }

    private static boolean isPageCompositionSource(Path path, String source) {
        String name = path.getFileName().toString();
        return name.endsWith("Page.java")
                || name.endsWith("PageHTML.java")
                || name.endsWith("PageCSS.java")
                || name.endsWith("PageAnimations.java");
    }

    private static boolean requiresGeneratedHTMLSymbols(Path path, String source) {
        String name = path.getFileName().toString();
        if (name.endsWith("PageCSS.java")) {
            return source.contains(".use(");
        }
        if (name.endsWith("PageHTML.java")) {
            return source.contains(".classes(") || source.contains(".id(") || source.contains(".hook(");
        }
        return false;
    }

    private static boolean importsAny(String source, List<String> packages) {
        for (String packageName : packages) {
            if (source.contains("import " + packageName + ".")
                    || source.contains("import " + packageName + ";")) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String source, List<String> markers) {
        return markers.stream().anyMatch(source::contains);
    }

    private static String packageName(String source) {
        Matcher matcher = Pattern.compile("(?m)^\\s*package\\s+([A-Za-z0-9_.]+)\\s*;").matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static ProjectLocation locate(Path sourceRoot) {
        Path moduleDirectory = nearestBuildDirectory(sourceRoot);
        if (moduleDirectory == null) {
            return null;
        }
        Path repositoryRoot = moduleDirectory;
        while (repositoryRoot != null && !hasSettings(repositoryRoot)) {
            repositoryRoot = repositoryRoot.getParent();
        }
        if (repositoryRoot == null) {
            return null;
        }
        String repositoryName = projectName(repositoryRoot);
        String moduleName = moduleDirectory.equals(repositoryRoot)
                ? repositoryName
                : moduleDirectory.getFileName().toString();
        return new ProjectLocation(repositoryName, moduleName, repositoryRoot, moduleDirectory);
    }

    private static Path nearestBuildDirectory(Path sourceRoot) {
        for (Path path = sourceRoot; path != null; path = path.getParent()) {
            if (Files.isRegularFile(path.resolve("build.gradle.kts"))
                    || Files.isRegularFile(path.resolve("build.gradle"))) {
                return path;
            }
        }
        return null;
    }

    private static boolean hasSettings(Path path) {
        return Files.isRegularFile(path.resolve("settings.gradle.kts"))
                || Files.isRegularFile(path.resolve("settings.gradle"));
    }

    private static String projectName(Path repositoryRoot) {
        for (String filename : List.of("settings.gradle.kts", "settings.gradle")) {
            Path path = repositoryRoot.resolve(filename);
            if (!Files.isRegularFile(path)) {
                continue;
            }
            Matcher matcher = ROOT_PROJECT_NAME.matcher(read(path));
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return repositoryRoot.getFileName().toString();
    }

    private static List<Path> javaSources(Path root) {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scan Java sources at " + root, exception);
        }
    }

    private static String readBuild(Path moduleDirectory) {
        for (String filename : List.of("build.gradle.kts", "build.gradle")) {
            Path build = moduleDirectory.resolve(filename);
            if (Files.isRegularFile(build)) {
                return read(build);
            }
        }
        return "";
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + path, exception);
        }
    }

    private static String subject(ProjectLocation location, Path path) {
        return location.repositoryRoot().relativize(path).toString().replace('\\', '/');
    }

    private static void add(
            List<ArchitectureViolation> violations,
            String ruleId,
            String subject,
            String message
    ) {
        violations.add(new ArchitectureViolation(ruleId, subject, message));
    }

    private record ProjectLocation(
            String repositoryName,
            String moduleName,
            Path repositoryRoot,
            Path moduleDirectory
    ) {
    }
}
