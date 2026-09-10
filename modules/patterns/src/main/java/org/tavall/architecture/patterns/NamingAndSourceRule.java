package org.tavall.architecture.patterns;

import org.tavall.architecture.core.ArchitectureContext;
import org.tavall.architecture.core.ArchitectureRule;
import org.tavall.architecture.core.ArchitectureViolation;
import org.tavall.architecture.core.ProductionClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class NamingAndSourceRule implements ArchitectureRule {
    private static final Pattern VAR_LOCAL = Pattern.compile("(?m)^\\s*var\\s+[A-Za-z_$][A-Za-z0-9_$]*\\s*=");
    private static final Pattern SHUTDOWN_HOOK_THREAD = Pattern.compile(
            "Runtime\\s*\\.\\s*getRuntime\\s*\\(\\s*\\)\\s*\\.\\s*addShutdownHook\\s*\\(\\s*new\\s+Thread\\s*\\("
    );
    private static final List<String> THREAD_CREATION = List.of(
            "new Thread(",
            "Thread.startVirtualThread(",
            "Thread.ofVirtual(",
            "Thread.ofPlatform("
    );

    @Override
    public String id() {
        return "shared-patterns";
    }

    @Override
    public List<ArchitectureViolation> validate(ArchitectureContext context) {
        List<ArchitectureViolation> violations = new ArrayList<>();
        for (ProductionClass productionClass : context.productionClasses()) {
            String simpleName = productionClass.className().substring(productionClass.className().lastIndexOf('.') + 1);
            if (simpleName.contains("$")) {
                simpleName = simpleName.substring(simpleName.lastIndexOf('$') + 1);
            }
            if (simpleName.endsWith("Manager")) {
                violations.add(new ArchitectureViolation(
                        "manager-type",
                        productionClass.className(),
                        "Tavall-owned production Manager types are legacy/external-API debt; use the actual role name"
                ));
            }
            if (simpleName.endsWith("Repository")
                    || simpleName.endsWith("RepositoryImpl")
                    || simpleName.endsWith("RepositoryAdapter")
                    || simpleName.endsWith("RepositoryStore")) {
                violations.add(new ArchitectureViolation(
                        "repository-type",
                        productionClass.className(),
                        "Tavall-owned production Repository types are migration debt; use Tavall Database or the real capability role"
                ));
            }
        }
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
            if (VAR_LOCAL.matcher(source).find()) {
                violations.add(new ArchitectureViolation(
                        "production-var",
                        subject,
                        "Production Java local variables must use explicit declared types"
                ));
            }
            String threadScanSource = SHUTDOWN_HOOK_THREAD.matcher(source).replaceAll("Runtime.getRuntime().addShutdownHook(");
            for (String marker : THREAD_CREATION) {
                if (threadScanSource.contains(marker)) {
                    violations.add(new ArchitectureViolation(
                            "direct-thread-creation",
                            subject,
                            "Ordinary production code must use Tavall concurrency/platform scheduling instead of " + marker
                    ));
                    break;
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read production source " + path, exception);
        }
    }
}
