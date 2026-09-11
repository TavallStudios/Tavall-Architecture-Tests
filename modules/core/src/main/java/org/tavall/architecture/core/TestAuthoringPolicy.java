package org.tavall.architecture.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public final class TestAuthoringPolicy {
    public static final String SCAFFOLD_METHOD_NAME = "generatedScaffoldRequiresBehaviorCoverage";
    public static final String SCAFFOLD_FAILURE_PREFIX = "Replace generated scaffold with behavior-oriented assertions for ";

    private TestAuthoringPolicy() {
    }

    public static boolean requiresDirectTest(ProductionClass productionClass) {
        if (!productionClass.loaded()) {
            return false;
        }
        Class<?> type = productionClass.type();
        int modifiers = type.getModifiers();
        if (type.isInterface()
                || type.isAnnotation()
                || type.isEnum()
                || type.isRecord()
                || type.isSynthetic()
                || type.isAnonymousClass()
                || type.isLocalClass()
                || type.isMemberClass()
                || Modifier.isAbstract(modifiers)) {
            return false;
        }
        return Arrays.stream(type.getDeclaredMethods())
                .anyMatch(TestAuthoringPolicy::isBehaviorMethod);
    }

    public static boolean requiresProductionEquivalentComposition(Class<?> type) {
        for (Annotation annotation : type.getAnnotations()) {
            if (annotation.annotationType().getName().equals("org.tavall.dependency.annotations.DelegatesTo")) {
                return true;
            }
        }
        return false;
    }

    public static String expectedTestClassName(String productionClassName) {
        String topLevel = topLevelClassName(productionClassName);
        return topLevel + "Test";
    }

    public static String expectedTestRelativePath(String productionClassName) {
        return expectedTestClassName(productionClassName).replace('.', '/') + ".java";
    }

    public static String renderScaffold(String productionClassName) {
        String topLevel = topLevelClassName(productionClassName);
        int separator = topLevel.lastIndexOf('.');
        String packageName = separator < 0 ? "" : topLevel.substring(0, separator);
        String simpleName = separator < 0 ? topLevel : topLevel.substring(separator + 1);
        StringBuilder source = new StringBuilder();
        if (!packageName.isBlank()) {
            source.append("package ").append(packageName).append(";\n\n");
        }
        source.append("import org.junit.jupiter.api.Test;\n\n")
                .append("import static org.junit.jupiter.api.Assertions.fail;\n\n")
                .append("final class ").append(simpleName).append("Test {\n")
                .append("    @Test\n")
                .append("    void ").append(SCAFFOLD_METHOD_NAME).append("() {\n")
                .append("        fail(\"").append(SCAFFOLD_FAILURE_PREFIX).append(simpleName).append("\");\n")
                .append("    }\n")
                .append("}\n");
        return source.toString();
    }

    private static boolean isBehaviorMethod(Method method) {
        int modifiers = method.getModifiers();
        if (method.isSynthetic() || method.isBridge()) {
            return false;
        }
        if (!Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers)) {
            return false;
        }
        if (method.getName().equals("main")
                && Modifier.isStatic(modifiers)
                && method.getParameterCount() == 1
                && method.getParameterTypes()[0] == String[].class) {
            return false;
        }
        if (method.getName().equals("toString") && method.getParameterCount() == 0) {
            return false;
        }
        if (method.getName().equals("hashCode") && method.getParameterCount() == 0) {
            return false;
        }
        if (method.getName().equals("equals") && method.getParameterCount() == 1) {
            return false;
        }
        String name = method.getName();
        if ((name.startsWith("get") || name.startsWith("is")) && method.getParameterCount() == 0) {
            return false;
        }
        return !name.startsWith("set") || method.getParameterCount() != 1;
    }

    private static String topLevelClassName(String className) {
        int nested = className.indexOf('$');
        return nested < 0 ? className : className.substring(0, nested);
    }
}
