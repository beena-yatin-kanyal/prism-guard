package io.prismguard.analyzers.spring;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.ClassModel;
import io.prismguard.api.model.FieldModel;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.model.MethodModel;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Analyzer(type = "excessive-autowiring")
public class ExcessiveAutowiringRule {

    private static final Set<String> SPRING_COMPONENT_ANNOTATIONS = Set.of(
            "Component", "Service", "Repository", "Controller", "RestController",
            "org.springframework.stereotype.Component",
            "org.springframework.stereotype.Service",
            "org.springframework.stereotype.Repository",
            "org.springframework.stereotype.Controller",
            "org.springframework.web.bind.annotation.RestController"
    );

    @Check
    public List<Finding> check(RuleContext context) {
        int maxInjectedDeps = context.parameter("maxInjectedDeps", 7);

        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(this::isSpringBean)
                .forEach(classModel -> {
                    int injectedCount = countInjectedDependencies(classModel);

                    if (injectedCount > maxInjectedDeps) {
                        findings.add(Finding.builder()
                                .ruleId("SPRING008")
                                .ruleName("SPRING008")
                                .severity(FindingSeverity.MEDIUM)
                                .category(RuleCategory.DEPENDENCY_INJECTION)
                                .message(String.format(
                                        "Bean '%s' has %d injected dependencies. "
                                                + "This suggests it has too many responsibilities.",
                                        classModel.getSimpleName(), injectedCount))
                                .recommendation("Refactor using Facade, decompose the class, "
                                        + "or group related dependencies into composed services.")
                                .filePath(classModel.getSourceFile() != null
                                        ? classModel.getSourceFile().toString()
                                        : classModel.getFullyQualifiedName())
                                .lineNumber(classModel.getBeginLine())
                                .columnNumber(0)
                                .codeSnippet(classModel.getSimpleName() + " (" + injectedCount + " deps)")
                                .build());
                    }
                });

        return findings;
    }

    private boolean isSpringBean(ClassModel classModel) {
        return classModel.isSpringComponent()
                || classModel.getAnnotations().stream().anyMatch(SPRING_COMPONENT_ANNOTATIONS::contains);
    }

    private int countInjectedDependencies(ClassModel classModel) {
        // Count fields annotated with @Autowired or @Inject
        long fieldInjected = classModel.getFields().stream()
                .filter(FieldModel::isInjected)
                .count();

        // Count constructor injection parameters for @Autowired constructors
        long constructorInjected = classModel.getMethods().stream()
                .filter(m -> isConstructor(m, classModel.getSimpleName()))
                .filter(m -> m.hasAnnotation("Autowired")
                        || m.hasAnnotation("org.springframework.beans.factory.annotation.Autowired")
                        || m.hasAnnotation("Inject")
                        || m.hasAnnotation("javax.inject.Inject")
                        || m.hasAnnotation("jakarta.inject.Inject"))
                .mapToLong(m -> m.getParameterTypes() != null ? m.getParameterTypes().size() : 0)
                .sum();

        // If there's only one constructor with no @Autowired, Spring auto-injects it (Spring 4.3+)
        // Count those constructor params too
        if (constructorInjected == 0 && fieldInjected == 0) {
            List<MethodModel> constructors = classModel.getMethods().stream()
                    .filter(m -> isConstructor(m, classModel.getSimpleName()))
                    .toList();
            if (constructors.size() == 1) {
                constructorInjected = constructors.get(0).getParameterTypes() != null
                        ? constructors.get(0).getParameterTypes().size()
                        : 0;
            }
        }

        return (int) (fieldInjected + constructorInjected);
    }

    private boolean isConstructor(MethodModel method, String className) {
        return method.getName().equals("<init>") || method.getName().equals(className);
    }
}
