package org.tavall.architecture.core;

import com.sun.source.tree.ClassTree;

import java.util.Objects;

public record JavaTypeSource(String className, JavaSourceUnit source, ClassTree tree) {
    public JavaTypeSource {
        Objects.requireNonNull(className, "className");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(tree, "tree");
    }

    public SourceLocation location() {
        return source.location(tree).orElseGet(() -> SourceLocation.file(source.relativePath()));
    }
}
