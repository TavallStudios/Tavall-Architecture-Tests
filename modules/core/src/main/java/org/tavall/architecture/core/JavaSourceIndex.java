package org.tavall.architecture.core;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class JavaSourceIndex {
    private final List<JavaSourceUnit> units;
    private final List<SourceDiagnostic> diagnostics;
    private final Map<String, JavaTypeSource> types;

    private JavaSourceIndex(List<JavaSourceUnit> units, List<SourceDiagnostic> diagnostics) {
        this.units = List.copyOf(units);
        this.diagnostics = List.copyOf(diagnostics);
        Map<String, JavaTypeSource> discovered = new LinkedHashMap<>();
        for (JavaSourceUnit unit : units) {
            for (JavaTypeSource type : unit.declaredTypes()) {
                discovered.putIfAbsent(type.className(), type);
            }
        }
        this.types = Map.copyOf(discovered);
    }

    public static JavaSourceIndex parse(List<Path> roots) {
        List<Path> normalizedRoots = roots.stream()
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .filter(Files::isDirectory)
                .sorted(Comparator.comparingInt((Path path) -> path.getNameCount()).reversed())
                .toList();
        List<Path> files = new ArrayList<>();
        for (Path root : normalizedRoots) {
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".java"))
                        .sorted()
                        .forEach(files::add);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to scan Java source root " + root, exception);
            }
        }
        if (files.isEmpty()) {
            return new JavaSourceIndex(List.of(), List.of());
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Canonical architecture source analysis requires a JDK compiler");
        }
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        List<JavaSourceUnit> units = new ArrayList<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                collector,
                Locale.ROOT,
                StandardCharsets.UTF_8
        )) {
            Iterable<? extends JavaFileObject> sources = fileManager.getJavaFileObjectsFromPaths(files);
            JavacTask task = (JavacTask) compiler.getTask(
                    null,
                    fileManager,
                    collector,
                    List.of("-proc:none"),
                    null,
                    sources
            );
            List<CompilationUnitTree> parsed = new ArrayList<>();
            for (CompilationUnitTree compilationUnit : task.parse()) {
                parsed.add(compilationUnit);
            }
            SourcePositions positions = Trees.instance(task).getSourcePositions();
            for (CompilationUnitTree compilationUnit : parsed) {
                Path path = Path.of(compilationUnit.getSourceFile().toUri()).toAbsolutePath().normalize();
                Path root = owningRoot(normalizedRoots, path);
                String relative = root == null
                        ? path.getFileName().toString()
                        : normalize(root.relativize(path).toString());
                units.add(new JavaSourceUnit(
                        path,
                        relative,
                        Files.readString(path),
                        compilationUnit,
                        positions
                ));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse Java source roots " + normalizedRoots, exception);
        }

        List<SourceDiagnostic> diagnostics = collector.getDiagnostics().stream()
                .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                .map(diagnostic -> toDiagnostic(diagnostic, normalizedRoots))
                .toList();
        units.sort(Comparator.comparing(JavaSourceUnit::relativePath));
        return new JavaSourceIndex(units, diagnostics);
    }

    public List<JavaSourceUnit> units() {
        return units;
    }

    public List<SourceDiagnostic> diagnostics() {
        return diagnostics;
    }

    public Optional<JavaSourceUnit> findByRelativePath(String relativePath) {
        String normalized = normalize(relativePath);
        return units.stream().filter(unit -> unit.relativePath().equals(normalized)).findFirst();
    }

    public Optional<JavaTypeSource> findType(String className) {
        return Optional.ofNullable(types.get(className));
    }

    public Optional<SourceLocation> locateClass(String className) {
        return findType(className).map(JavaTypeSource::location);
    }

    public Optional<String> classNameForRelativePath(String relativePath) {
        return findByRelativePath(relativePath).flatMap(unit -> unit.declaredTypes().stream()
                .map(JavaTypeSource::className)
                .filter(name -> !name.contains("$"))
                .findFirst());
    }

    private static SourceDiagnostic toDiagnostic(
            Diagnostic<? extends JavaFileObject> diagnostic,
            List<Path> roots
    ) {
        String path = "unknown.java";
        if (diagnostic.getSource() != null) {
            URI uri = diagnostic.getSource().toUri();
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                Path sourcePath = Path.of(uri).toAbsolutePath().normalize();
                Path root = owningRoot(roots, sourcePath);
                path = root == null
                        ? sourcePath.getFileName().toString()
                        : normalize(root.relativize(sourcePath).toString());
            }
        }
        int line = positive(diagnostic.getLineNumber());
        int column = positive(diagnostic.getColumnNumber());
        return new SourceDiagnostic(
                diagnostic.getMessage(Locale.ROOT),
                new SourceLocation(path, line, column, line, column)
        );
    }

    private static Path owningRoot(List<Path> roots, Path file) {
        return roots.stream().filter(file::startsWith).findFirst().orElse(null);
    }

    private static int positive(long value) {
        return value > 0 ? Math.toIntExact(value) : 1;
    }

    private static String normalize(String path) {
        return path.replace('\\', '/');
    }
}
