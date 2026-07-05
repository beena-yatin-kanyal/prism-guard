package io.prismguard.analyzers.spring;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Analyzer(type = "field-injection")
public class FieldInjectionRule {

    private static final Set<String> TEST_SUFFIXES = Set.of("Test", "Tests", "Spec", "IT");

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(classModel -> !isTestClass(classModel.getSimpleName()))
                .filter(classModel -> classModel.isSpringComponent())
                .forEach(classModel -> {
                    String className = classModel.getSimpleName();
                    classModel.getFields().forEach(field -> {
                        boolean isFieldInjected = field.hasAnnotation("Autowired")
                                || field.hasAnnotation("Inject")
                                || field.hasAnnotation("org.springframework.beans.factory.annotation.Autowired")
                                || field.hasAnnotation("javax.inject.Inject")
                                || field.hasAnnotation("jakarta.inject.Inject");

                        if (isFieldInjected) {
                            findings.add(Finding.builder()
                                    .ruleId("SPRING001")
                                    .ruleName("SPRING001")
                                    .severity(FindingSeverity.HIGH)
                                    .category(RuleCategory.DEPENDENCY_INJECTION)
                                    .message(String.format(
                                            "Class '%s' uses field injection for '%s'. "
                                                    + "Prefer constructor injection.",
                                            className, field.name()))
                                    .recommendation("Constructor injection is preferred: immutable, "
                                            + "testable, and explicit dependencies")
                                    .filePath(classModel.getSourceFile() != null
                                            ? classModel.getSourceFile().toString()
                                            : classModel.getFullyQualifiedName())
                                    .lineNumber(classModel.getBeginLine())
                                    .columnNumber(0)
                                    .codeSnippet("@Autowired " + field.type() + " " + field.name())
                                    .build());
                        }
                    });
                });

        return findings;
    }

    private boolean isTestClass(String simpleName) {
        return TEST_SUFFIXES.stream().anyMatch(simpleName::endsWith);
    }
}
