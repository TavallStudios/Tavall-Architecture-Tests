package org.tavall.architecture.database;

import org.tavall.architecture.core.ArchitectureContext;
import org.tavall.architecture.core.ArchitectureRule;
import org.tavall.architecture.core.ArchitectureViolation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class DatabaseRule implements ArchitectureRule {
    private static final Map<String, String> FORBIDDEN_APPLICATION_BOUNDARIES = Map.of(
            "jakarta.persistence.EntityManager", "Application code must not own EntityManager lifecycle",
            "jakarta.persistence.EntityManagerFactory", "Application code must not own EntityManagerFactory lifecycle",
            "java.sql.DriverManager", "Application code must not open raw JDBC connections through DriverManager"
    );

    @Override
    public String id() {
        return "tavall-database";
    }

    @Override
    public List<ArchitectureViolation> validate(ArchitectureContext context) {
        List<ArchitectureViolation> violations = new ArrayList<>();
        for (Path sourceRoot : context.sourceRoots()) {
            scanSources(sourceRoot, violations);
        }
        return List.copyOf(violations);
    }

    private static void scanSources(Path root, List<ArchitectureViolation> violations) {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .forEach(path -> inspectSource(root, path, violations));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scan Java source root " + root, exception);
        }
    }

    private static void inspectSource(Path root, Path path, List<ArchitectureViolation> violations) {
        try {
            String source = Files.readString(path);
            String subject = root.relativize(path).toString().replace('\\', '/');
            FORBIDDEN_APPLICATION_BOUNDARIES.forEach((typeName, message) -> {
                if (source.contains("import " + typeName + ";") || source.contains(typeName + ".")) {
                    violations.add(new ArchitectureViolation(
                            "database-direct-boundary",
                            subject + "->" + typeName,
                            message + "; use the checked-in Tavall Database entity contract"
                    ));
                }
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read production source " + path, exception);
        }
    }
}
