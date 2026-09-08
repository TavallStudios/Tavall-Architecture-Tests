package org.tavall.architecture;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TavallRepositoryTypeNameArchitectureTest {
    private static final String REPOSITORY_MIGRATION_CUTOFF =
            "705a17b7db22a6012f3cdefe99328129842301a0";
    private static final String PROHIBITED_SUFFIX = "Repository";

    @Test
    void newProductionRepositoryTypeNamesAreProhibited() throws Exception {
        Path repositoryRoot = findGitRepositoryRoot();
        verifyMigrationCutoffExists(repositoryRoot);

        List<String> changedProductionJavaFiles =
                changedProductionJavaFiles(repositoryRoot);
        List<String> violations = new ArrayList<>();

        for (String sourcePath : changedProductionJavaFiles) {
            Path currentSourcePath = repositoryRoot.resolve(sourcePath);
            if (!Files.isRegularFile(currentSourcePath)) {
                continue;
            }

            String currentSource = Files.readString(
                    currentSourcePath,
                    StandardCharsets.UTF_8
            );
            Set<String> currentRepositoryTypes =
                    declaredRepositoryTypeNames(currentSource, sourcePath);
            if (currentRepositoryTypes.isEmpty()) {
                continue;
            }

            GitResult cutoffSourceResult = runGit(
                    repositoryRoot,
                    "show",
                    REPOSITORY_MIGRATION_CUTOFF + ":" + sourcePath
            );
            Set<String> cutoffRepositoryTypes = cutoffSourceResult.success()
                    ? declaredRepositoryTypeNames(
                            cutoffSourceResult.output(),
                            sourcePath
                    )
                    : Set.of();

            for (String typeName : currentRepositoryTypes) {
                if (!cutoffRepositoryTypes.contains(typeName)) {
                    violations.add(sourcePath + " -> " + typeName);
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "New Tavall production type names ending in `Repository` are prohibited. "
                        + "Existing declarations at migration cutoff "
                        + REPOSITORY_MIGRATION_CUTOFF
                        + " are legacy debt only and may be removed/renamed, not expanded. "
                        + "Model durable persistence with Tavall Database entity classes and "
                        + "the current Tavall Database entity contract. Violations:\n"
                        + String.join(System.lineSeparator(), violations)
        );
    }

    private Path findGitRepositoryRoot() throws Exception {
        Path currentDirectory = Path.of("").toAbsolutePath().normalize();
        GitResult result = runGit(currentDirectory, "rev-parse", "--show-toplevel");

        assertTrue(
                result.success(),
                () -> "Architecture Repository-name enforcement requires a Git working tree. "
                        + result.output()
        );

        return Path.of(result.output().trim()).toAbsolutePath().normalize();
    }

    private void verifyMigrationCutoffExists(Path repositoryRoot) throws Exception {
        GitResult result = runGit(
                repositoryRoot,
                "cat-file",
                "-e",
                REPOSITORY_MIGRATION_CUTOFF + "^{commit}"
        );

        assertTrue(
                result.success(),
                () -> "Repository-name migration cutoff is unavailable in this checkout: "
                        + REPOSITORY_MIGRATION_CUTOFF
                        + ". Architecture validation requires Git history containing the cutoff. "
                        + result.output()
        );
    }

    private List<String> changedProductionJavaFiles(Path repositoryRoot)
            throws Exception {
        GitResult result = runGit(
                repositoryRoot,
                "diff",
                "--name-only",
                "--diff-filter=ACMR",
                REPOSITORY_MIGRATION_CUTOFF + "..HEAD",
                "--"
        );

        assertTrue(
                result.success(),
                () -> "Unable to compare production sources with Repository migration cutoff. "
                        + result.output()
        );

        return result.output().lines()
                .map(String::trim)
                .filter(path -> !path.isEmpty())
                .filter(this::isProductionJavaSource)
                .toList();
    }

    private boolean isProductionJavaSource(String sourcePath) {
        String normalizedPath = sourcePath.replace('\\', '/');
        return normalizedPath.endsWith(".java")
                && (normalizedPath.startsWith("src/main/java/")
                || normalizedPath.contains("/src/main/java/"));
    }

    private Set<String> declaredRepositoryTypeNames(
            String source,
            String sourcePath
    ) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException(
                    "Architecture Repository-name enforcement requires a JDK compiler"
            );
        }

        SimpleJavaFileObject sourceObject = new StringJavaSource(
                sourcePath,
                source
        );
        JavacTask task = (JavacTask) compiler.getTask(
                null,
                null,
                null,
                List.of("-proc:none"),
                null,
                List.of(sourceObject)
        );

        Set<String> typeNames = new LinkedHashSet<>();
        Iterable<? extends CompilationUnitTree> compilationUnits = task.parse();

        TreeScanner<Void, Void> scanner = new TreeScanner<>() {
            @Override
            public Void visitClass(ClassTree classTree, Void unused) {
                String simpleName = classTree.getSimpleName().toString();
                if (simpleName.endsWith(PROHIBITED_SUFFIX)) {
                    typeNames.add(simpleName);
                }
                return super.visitClass(classTree, unused);
            }
        };

        for (CompilationUnitTree compilationUnit : compilationUnits) {
            scanner.scan(compilationUnit, null);
        }

        return Set.copyOf(typeNames);
    }

    private GitResult runGit(Path directory, String... arguments)
            throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));

        Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start();

        String output;
        try (var inputStream = process.getInputStream()) {
            output = new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }

        int exitCode = process.waitFor();
        return new GitResult(exitCode, output);
    }

    private record GitResult(int exitCode, String output) {
        private boolean success() {
            return exitCode == 0;
        }
    }

    private static final class StringJavaSource extends SimpleJavaFileObject {
        private final String source;

        private StringJavaSource(String sourcePath, String source) {
            super(
                    URI.create(
                            "string:///"
                                    + sourcePath.replace('\\', '/')
                            ),
                    Kind.SOURCE
            );
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
