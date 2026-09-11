package org.tavall.architecture.patterns;

import org.tavall.architecture.core.ArchitectureContext;
import org.tavall.architecture.core.ArchitectureFinding;
import org.tavall.architecture.core.ArchitectureRule;
import org.tavall.architecture.core.ArchitectureSeverity;
import org.tavall.architecture.core.ArchitectureViolation;
import org.tavall.architecture.core.JavaSourceUnit;
import org.tavall.architecture.core.ProductionClass;
import org.tavall.architecture.core.SourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        return inspect(context).stream().map(ArchitectureFinding::toViolation).toList();
    }

    @Override
    public List<ArchitectureFinding> inspect(ArchitectureContext context) {
        List<ArchitectureFinding> findings = new ArrayList<>();
        for (ProductionClass productionClass : context.productionClasses()) {
            String simpleName = productionClass.className().substring(productionClass.className().lastIndexOf('.') + 1);
            if (simpleName.contains("$")) {
                simpleName = simpleName.substring(simpleName.lastIndexOf('$') + 1);
            }
            if (simpleName.endsWith("Manager")) {
                findings.add(classFinding(
                        context,
                        "manager-type",
                        productionClass.className(),
                        "Tavall-owned production Manager types are legacy/external-API debt; use the actual role name"
                ));
            }
            if (simpleName.endsWith("Repository")
                    || simpleName.endsWith("RepositoryImpl")
                    || simpleName.endsWith("RepositoryAdapter")
                    || simpleName.endsWith("RepositoryStore")) {
                findings.add(classFinding(
                        context,
                        "repository-type",
                        productionClass.className(),
                        "Tavall-owned production Repository types are migration debt; use Tavall Database or the real capability role"
                ));
            }
        }
        for (JavaSourceUnit source : context.productionSources().units()) {
            inspectSource(context, source, findings);
        }
        return List.copyOf(findings);
    }

    private ArchitectureFinding classFinding(
            ArchitectureContext context,
            String ruleId,
            String className,
            String message
    ) {
        return new ArchitectureFinding(
                id(),
                ruleId,
                className,
                className,
                message,
                ArchitectureSeverity.BLOCKING,
                context.productionSources().locateClass(className).orElse(null)
        );
    }

    private void inspectSource(
            ArchitectureContext context,
            JavaSourceUnit source,
            List<ArchitectureFinding> findings
    ) {
        String text = source.source();
        Matcher varMatcher = VAR_LOCAL.matcher(text);
        if (varMatcher.find()) {
            findings.add(sourceFinding(
                    context,
                    source,
                    "production-var",
                    "Production Java local variables must use explicit declared types",
                    source.locationAtOffsets(varMatcher.start(), varMatcher.end())
            ));
        }

        List<int[]> allowedShutdownThreadRanges = new ArrayList<>();
        Matcher shutdownMatcher = SHUTDOWN_HOOK_THREAD.matcher(text);
        while (shutdownMatcher.find()) {
            allowedShutdownThreadRanges.add(new int[]{shutdownMatcher.start(), shutdownMatcher.end()});
        }
        for (String marker : THREAD_CREATION) {
            int searchFrom = 0;
            while (searchFrom < text.length()) {
                int index = text.indexOf(marker, searchFrom);
                if (index < 0) {
                    break;
                }
                searchFrom = index + marker.length();
                if (marker.equals("new Thread(") && inAllowedShutdownRange(index, allowedShutdownThreadRanges)) {
                    continue;
                }
                findings.add(sourceFinding(
                        context,
                        source,
                        "direct-thread-creation",
                        "Ordinary production code must use Tavall concurrency/platform scheduling instead of " + marker,
                        source.locationAtOffsets(index, index + marker.length())
                ));
                return;
            }
        }
    }

    private ArchitectureFinding sourceFinding(
            ArchitectureContext context,
            JavaSourceUnit source,
            String ruleId,
            String message,
            SourceLocation location
    ) {
        String className = context.productionSources().classNameForRelativePath(source.relativePath()).orElse(null);
        return new ArchitectureFinding(
                id(),
                ruleId,
                source.relativePath(),
                className,
                message,
                ArchitectureSeverity.BLOCKING,
                location
        );
    }

    private static boolean inAllowedShutdownRange(int index, List<int[]> ranges) {
        for (int[] range : ranges) {
            if (index >= range[0] && index < range[1]) {
                return true;
            }
        }
        return false;
    }
}
