package org.tavall.architecture.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class ArchitectureContext {
    private final List<Path> classRoots;
    private final List<Path> sourceRoots;
    private final ClassLoader classLoader;

    private ArchitectureContext(List<Path> classRoots, List<Path> sourceRoots, ClassLoader classLoader) {
        this.classRoots = List.copyOf(classRoots);
        this.sourceRoots = List.copyOf(sourceRoots);
        this.classLoader = classLoader;
    }

    public static ArchitectureContext fromSystemProperties() {
        List<Path> classRoots = parseRoots("tavall.architecture.classRoots", true);
        List<Path> sourceRoots = parseRoots("tavall.architecture.sourceRoots", false);
        return new ArchitectureContext(classRoots, sourceRoots, Thread.currentThread().getContextClassLoader());
    }

    public List<Path> classRoots() {
        return classRoots;
    }

    public List<Path> sourceRoots() {
        return sourceRoots;
    }

    public ClassLoader classLoader() {
        return classLoader;
    }

    public List<ProductionClass> productionClasses() {
        Map<String, List<Path>> origins = new LinkedHashMap<>();
        for (Path root : classRoots) {
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".class"))
                        .sorted(Comparator.naturalOrder())
                        .forEach(path -> {
                            String relative = root.relativize(path).toString();
                            String className = relative.substring(0, relative.length() - 6)
                                    .replace(File.separatorChar, '.');
                            if (className.startsWith("org.tavall.")
                                    && !className.endsWith("module-info")
                                    && !className.endsWith("package-info")) {
                                origins.computeIfAbsent(className, ignored -> new ArrayList<>()).add(path);
                            }
                        });
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to scan compiled class root " + root, exception);
            }
        }

        return origins.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> {
            try {
                return new ProductionClass(
                        entry.getKey(),
                        entry.getValue(),
                        Class.forName(entry.getKey(), false, classLoader),
                        null
                );
            } catch (Throwable failure) {
                return new ProductionClass(entry.getKey(), entry.getValue(), null, failure);
            }
        }).toList();
    }

    private static List<Path> parseRoots(String property, boolean required) {
        String value = System.getProperty(property, "");
        if (required && value.isBlank()) {
            throw new IllegalStateException("Missing system property " + property);
        }
        if (value.isBlank()) {
            return List.of();
        }
        return Stream.of(value.split(java.util.regex.Pattern.quote(File.pathSeparator)))
                .filter(raw -> !raw.isBlank())
                .map(Path::of)
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .filter(Files::isDirectory)
                .toList();
    }
}
