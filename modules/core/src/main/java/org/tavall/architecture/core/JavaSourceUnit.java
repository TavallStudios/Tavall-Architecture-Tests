package org.tavall.architecture.core;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JavaSourceUnit {
    private final Path path;
    private final String relativePath;
    private final String source;
    private final CompilationUnitTree compilationUnit;
    private final SourcePositions sourcePositions;

    JavaSourceUnit(
            Path path,
            String relativePath,
            String source,
            CompilationUnitTree compilationUnit,
            SourcePositions sourcePositions
    ) {
        this.path = Objects.requireNonNull(path, "path");
        this.relativePath = Objects.requireNonNull(relativePath, "relativePath");
        this.source = Objects.requireNonNull(source, "source");
        this.compilationUnit = Objects.requireNonNull(compilationUnit, "compilationUnit");
        this.sourcePositions = Objects.requireNonNull(sourcePositions, "sourcePositions");
    }

    public Path path() {
        return path;
    }

    public String relativePath() {
        return relativePath;
    }

    public String source() {
        return source;
    }

    public CompilationUnitTree compilationUnit() {
        return compilationUnit;
    }

    public String packageName() {
        return compilationUnit.getPackageName() == null ? "" : compilationUnit.getPackageName().toString();
    }

    public Optional<SourceLocation> location(Tree tree) {
        long start = sourcePositions.getStartPosition(compilationUnit, tree);
        long end = sourcePositions.getEndPosition(compilationUnit, tree);
        if (start < 0) {
            return Optional.empty();
        }
        if (end < start) {
            end = start;
        }
        return Optional.of(locationAtOffsets(start, end));
    }

    public SourceLocation locationAtOffsets(long start, long end) {
        long boundedStart = Math.max(0, Math.min(start, source.length()));
        long boundedEnd = Math.max(boundedStart, Math.min(end, source.length()));
        int startLine = safeCoordinate(compilationUnit.getLineMap().getLineNumber(boundedStart));
        int startColumn = safeCoordinate(compilationUnit.getLineMap().getColumnNumber(boundedStart));
        int endLine = safeCoordinate(compilationUnit.getLineMap().getLineNumber(boundedEnd));
        int endColumn = safeCoordinate(compilationUnit.getLineMap().getColumnNumber(boundedEnd));
        return new SourceLocation(relativePath, startLine, startColumn, endLine, endColumn);
    }

    public Optional<SourceLocation> markerLocation(String marker) {
        int index = source.indexOf(marker);
        if (index < 0) {
            return Optional.empty();
        }
        return Optional.of(locationAtOffsets(index, index + marker.length()));
    }

    public List<JavaTypeSource> declaredTypes() {
        List<JavaTypeSource> declared = new ArrayList<>();
        String packagePrefix = packageName().isBlank() ? "" : packageName() + ".";
        new TreeScanner<Void, String>() {
            @Override
            public Void visitClass(ClassTree node, String parentBinaryName) {
                String simpleName = node.getSimpleName().toString();
                String binaryName;
                if (simpleName.isBlank()) {
                    binaryName = parentBinaryName;
                } else if (parentBinaryName == null || parentBinaryName.isBlank()) {
                    binaryName = packagePrefix + simpleName;
                } else {
                    binaryName = parentBinaryName + "$" + simpleName;
                }
                if (!simpleName.isBlank()) {
                    declared.add(new JavaTypeSource(binaryName, JavaSourceUnit.this, node));
                }
                return super.visitClass(node, binaryName);
            }
        }.scan(compilationUnit, null);
        return List.copyOf(declared);
    }

    public Optional<JavaTypeSource> findDeclaredType(String binaryName) {
        return declaredTypes().stream().filter(type -> type.className().equals(binaryName)).findFirst();
    }

    private static int safeCoordinate(long value) {
        if (value <= 0) {
            return 1;
        }
        return Math.toIntExact(value);
    }
}
