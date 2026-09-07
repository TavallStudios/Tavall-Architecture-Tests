package org.tavall.architecture;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

final class TavallProductionClassIndex {
    private static final String CLASS_ROOTS_PROPERTY = "tavall.architecture.classRoots";

    List<TavallProductionClass> discover() {
        String configuredRoots = System.getProperty(CLASS_ROOTS_PROPERTY, "");
        if (configuredRoots.isBlank()) {
            throw new IllegalStateException(
                    "Missing system property " + CLASS_ROOTS_PROPERTY
            );
        }

        Map<String, List<Path>> originsByClass = new LinkedHashMap<>();
        for (String rawRoot : configuredRoots.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (rawRoot.isBlank()) {
                continue;
            }
            Path root = Path.of(rawRoot).toAbsolutePath().normalize();
            if (!Files.isDirectory(root)) {
                continue;
            }
            discoverRoot(root, originsByClass);
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        List<TavallProductionClass> classes = new ArrayList<>();
        originsByClass.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> classes.add(load(
                        entry.getKey(),
                        List.copyOf(entry.getValue()),
                        classLoader
                )));
        return List.copyOf(classes);
    }

    private void discoverRoot(
            Path root,
            Map<String, List<Path>> originsByClass
    ) {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".class"))
                    .map(path -> new ClassFile(root, path))
                    .filter(classFile -> classFile.className().startsWith("org.tavall."))
                    .filter(classFile -> !classFile.className().endsWith("module-info"))
                    .filter(classFile -> !classFile.className().endsWith("package-info"))
                    .sorted(Comparator.comparing(ClassFile::className))
                    .forEach(classFile -> originsByClass
                            .computeIfAbsent(classFile.className(), ignored -> new ArrayList<>())
                            .add(classFile.path()));
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to scan compiled class root " + root,
                    exception
            );
        }
    }

    private TavallProductionClass load(
            String className,
            List<Path> origins,
            ClassLoader classLoader
    ) {
        try {
            Class<?> type = Class.forName(className, false, classLoader);
            return new TavallProductionClass(className, origins, type, null);
        } catch (Throwable failure) {
            return new TavallProductionClass(className, origins, null, failure);
        }
    }

    private record ClassFile(Path root, Path path) {
        private ClassFile {
            Objects.requireNonNull(root, "root");
            Objects.requireNonNull(path, "path");
        }

        String className() {
            String relative = root.relativize(path).toString();
            return relative.substring(0, relative.length() - ".class".length())
                    .replace(File.separatorChar, '.');
        }
    }

    record TavallProductionClass(
            String className,
            List<Path> origins,
            Class<?> type,
            Throwable loadFailure
    ) {
        TavallProductionClass {
            Objects.requireNonNull(className, "className");
            origins = List.copyOf(origins);
        }

        boolean loaded() {
            return type != null && loadFailure == null;
        }
    }
}
