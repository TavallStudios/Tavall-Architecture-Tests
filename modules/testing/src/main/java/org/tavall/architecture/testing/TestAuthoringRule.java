package org.tavall.architecture.testing;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import org.tavall.architecture.core.ArchitectureContext;
import org.tavall.architecture.core.ArchitectureFinding;
import org.tavall.architecture.core.ArchitectureRule;
import org.tavall.architecture.core.ArchitectureSeverity;
import org.tavall.architecture.core.ArchitectureViolation;
import org.tavall.architecture.core.JavaSourceUnit;
import org.tavall.architecture.core.JavaTypeSource;
import org.tavall.architecture.core.ProductionClass;
import org.tavall.architecture.core.SourceDiagnostic;
import org.tavall.architecture.core.SourceLocation;
import org.tavall.architecture.core.TestAuthoringPolicy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class TestAuthoringRule implements ArchitectureRule {
    private static final Set<String> JUPITER_TEST_ANNOTATIONS = Set.of(
            "Test",
            "ParameterizedTest",
            "RepeatedTest",
            "TestFactory",
            "TestTemplate"
    );

    @Override
    public String id() {
        return "test-authoring";
    }

    @Override
    public List<ArchitectureViolation> validate(ArchitectureContext context) {
        return inspect(context).stream().map(ArchitectureFinding::toViolation).toList();
    }

    @Override
    public List<ArchitectureFinding> inspect(ArchitectureContext context) {
        List<ArchitectureFinding> findings = new ArrayList<>();
        for (SourceDiagnostic diagnostic : context.testSources().diagnostics()) {
            findings.add(new ArchitectureFinding(
                    id(),
                    "test-source-syntax",
                    diagnostic.location().path() + "#" + diagnostic.location().startLine()
                            + ":" + diagnostic.location().startColumn(),
                    context.testSources().classNameForRelativePath(diagnostic.location().path())
                            .flatMap(name -> productionNameForTest(context, name))
                            .orElse(null),
                    "Test source must parse as Java before it can provide architecture evidence: " + diagnostic.message(),
                    ArchitectureSeverity.BLOCKING,
                    diagnostic.location(),
                    "tavall-docs/docs/quality/code-architecture/TESTING_AND_GIT.md"
            ));
        }

        for (ProductionClass productionClass : context.productionClasses()) {
            if (!TestAuthoringPolicy.requiresDirectTest(productionClass)) {
                continue;
            }
            String expectedTestClass = TestAuthoringPolicy.expectedTestClassName(productionClass.className());
            JavaTypeSource testType = context.testSources().findType(expectedTestClass).orElse(null);
            if (testType == null) {
                findings.add(new ArchitectureFinding(
                        id(),
                        "missing-direct-test",
                        productionClass.className(),
                        productionClass.className(),
                        "Behavior-bearing production type requires direct JUnit 5 coverage at "
                                + TestAuthoringPolicy.expectedTestRelativePath(productionClass.className()),
                        ArchitectureSeverity.BLOCKING,
                        context.productionSources().locateClass(productionClass.className()).orElse(null),
                        "tavall-docs/docs/quality/code-architecture/TESTING_AND_GIT.md"
                ));
                continue;
            }
            inspectDirectTest(context, productionClass, testType, findings);
        }
        return List.copyOf(findings);
    }

    private void inspectDirectTest(
            ArchitectureContext context,
            ProductionClass productionClass,
            JavaTypeSource testType,
            List<ArchitectureFinding> findings
    ) {
        String productionName = productionClass.className();
        String simpleProductionName = productionClass.type().getSimpleName();
        JavaSourceUnit unit = testType.source();
        String expectedPath = TestAuthoringPolicy.expectedTestRelativePath(productionName);
        if (!unit.relativePath().equals(expectedPath)) {
            findings.add(finding(
                    "test-source-path",
                    productionName + "#test-path",
                    productionName,
                    "Direct test must match the production package/path; expected " + expectedPath,
                    testType.location()
            ));
        }

        Set<String> imports = new LinkedHashSet<>();
        for (ImportTree importTree : unit.compilationUnit().getImports()) {
            imports.add(importTree.getQualifiedIdentifier().toString());
        }
        boolean junit4Imported = imports.contains("org.junit.Test");
        Set<String> jupiterAnnotations = new LinkedHashSet<>();
        for (String imported : imports) {
            if (imported.startsWith("org.junit.jupiter.") || imported.startsWith("org.junit.jupiter.params.")) {
                int separator = imported.lastIndexOf('.');
                if (separator >= 0) {
                    String simple = imported.substring(separator + 1);
                    if (JUPITER_TEST_ANNOTATIONS.contains(simple)) {
                        jupiterAnnotations.add(simple);
                    }
                }
            }
        }

        int executableTests = 0;
        for (Tree member : testType.tree().getMembers()) {
            if (!(member instanceof MethodTree method)) {
                continue;
            }
            String annotation = testAnnotation(method, jupiterAnnotations, junit4Imported);
            if (annotation == null) {
                continue;
            }
            if (annotation.equals("junit4")) {
                findings.add(finding(
                        "junit4-test",
                        productionName + "#junit4:" + method.getName(),
                        productionName,
                        "Direct tests must use JUnit 5 rather than org.junit.Test",
                        unit.location(method).orElse(testType.location())
                ));
                continue;
            }
            executableTests++;
            String methodName = method.getName().toString();
            if (methodName.startsWith("test")) {
                findings.add(finding(
                        "test-method-name",
                        productionName + "#test-name:" + methodName,
                        productionName,
                        "Test method names must describe behavior rather than use the test* prefix",
                        unit.location(method).orElse(testType.location())
                ));
            }
            if (methodName.equals(TestAuthoringPolicy.SCAFFOLD_METHOD_NAME)) {
                findings.add(finding(
                        "generated-test-incomplete",
                        productionName + "#generated-scaffold",
                        productionName,
                        "Generated test scaffold is incomplete; replace it with real behavior assertions",
                        unit.location(method).orElse(testType.location())
                ));
            }
        }
        if (executableTests == 0) {
            findings.add(finding(
                    "missing-junit5-test-method",
                    productionName + "#junit5-methods",
                    productionName,
                    "Direct test class must contain at least one JUnit 5 behavior test method",
                    testType.location()
            ));
        }

        TestTreeScanner scanner = new TestTreeScanner(unit, productionName, simpleProductionName);
        scanner.scan(testType.tree(), null);
        if (scanner.mockLocation != null) {
            findings.add(finding(
                    "mock-subject-under-test",
                    productionName + "#mock-subject",
                    productionName,
                    "Do not mock the production type being tested",
                    scanner.mockLocation
            ));
        }
        if (scanner.privateReflectionLocation != null) {
            findings.add(finding(
                    "private-method-test",
                    productionName + "#private-reflection",
                    productionName,
                    "Tests must use the public production contract rather than reflectively testing private methods",
                    scanner.privateReflectionLocation
            ));
        }
        if (TestAuthoringPolicy.requiresProductionEquivalentComposition(productionClass.type())
                && scanner.directConstructionLocation != null) {
            findings.add(finding(
                    "test-di-composition",
                    productionName + "#di-composition",
                    productionName,
                    "Tavall-managed behavior must be composed through the production-equivalent DI path in tests",
                    scanner.directConstructionLocation
            ));
        }
    }

    private ArchitectureFinding finding(
            String ruleId,
            String subject,
            String className,
            String message,
            SourceLocation location
    ) {
        return new ArchitectureFinding(
                id(),
                ruleId,
                subject,
                className,
                message,
                ArchitectureSeverity.BLOCKING,
                location,
                "tavall-docs/docs/quality/code-architecture/TESTING_AND_GIT.md"
        );
    }

    private static String testAnnotation(
            MethodTree method,
            Set<String> jupiterAnnotations,
            boolean junit4Imported
    ) {
        for (AnnotationTree annotation : method.getModifiers().getAnnotations()) {
            String name = annotation.getAnnotationType().toString();
            if (name.equals("org.junit.Test") || (name.equals("Test") && junit4Imported && !jupiterAnnotations.contains("Test"))) {
                return "junit4";
            }
            String simpleName = name.substring(name.lastIndexOf('.') + 1);
            if ((name.startsWith("org.junit.jupiter.") || name.startsWith("org.junit.jupiter.params."))
                    && JUPITER_TEST_ANNOTATIONS.contains(simpleName)) {
                return "jupiter";
            }
            if (jupiterAnnotations.contains(simpleName)) {
                return "jupiter";
            }
        }
        return null;
    }

    private static java.util.Optional<String> productionNameForTest(ArchitectureContext context, String testClassName) {
        if (!testClassName.endsWith("Test")) {
            return java.util.Optional.empty();
        }
        String productionName = testClassName.substring(0, testClassName.length() - 4);
        return context.productionClasses().stream()
                .map(ProductionClass::className)
                .filter(productionName::equals)
                .findFirst();
    }

    private static final class TestTreeScanner extends TreeScanner<Void, Void> {
        private final JavaSourceUnit unit;
        private final String productionName;
        private final String simpleProductionName;
        private SourceLocation mockLocation;
        private SourceLocation privateReflectionLocation;
        private SourceLocation directConstructionLocation;

        private TestTreeScanner(JavaSourceUnit unit, String productionName, String simpleProductionName) {
            this.unit = unit;
            this.productionName = productionName;
            this.simpleProductionName = simpleProductionName;
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
            String select = node.getMethodSelect().toString();
            if (mockLocation == null && (select.equals("mock") || select.endsWith(".mock"))) {
                boolean mocksSubject = node.getArguments().stream().map(Object::toString).anyMatch(argument ->
                        argument.equals(simpleProductionName + ".class")
                                || argument.equals(productionName + ".class")
                );
                if (mocksSubject) {
                    mockLocation = unit.location(node).orElse(null);
                }
            }
            if (privateReflectionLocation == null && select.endsWith(".getDeclaredMethod")) {
                String expression = select.substring(0, select.length() - ".getDeclaredMethod".length());
                if (expression.equals(simpleProductionName + ".class")
                        || expression.equals(productionName + ".class")) {
                    privateReflectionLocation = unit.location(node).orElse(null);
                }
            }
            return super.visitMethodInvocation(node, unused);
        }

        @Override
        public Void visitVariable(VariableTree node, Void unused) {
            if (mockLocation == null && typeMatchesSubject(node.getType() == null ? "" : node.getType().toString())) {
                boolean mockAnnotation = node.getModifiers().getAnnotations().stream()
                        .map(annotation -> annotation.getAnnotationType().toString())
                        .anyMatch(name -> name.equals("Mock") || name.equals("org.mockito.Mock"));
                if (mockAnnotation) {
                    mockLocation = unit.location(node).orElse(null);
                }
            }
            return super.visitVariable(node, unused);
        }

        @Override
        public Void visitNewClass(NewClassTree node, Void unused) {
            if (directConstructionLocation == null && typeMatchesSubject(node.getIdentifier().toString())) {
                directConstructionLocation = unit.location(node).orElse(null);
            }
            return super.visitNewClass(node, unused);
        }

        private boolean typeMatchesSubject(String typeName) {
            return typeName.equals(simpleProductionName) || typeName.equals(productionName);
        }
    }
}
