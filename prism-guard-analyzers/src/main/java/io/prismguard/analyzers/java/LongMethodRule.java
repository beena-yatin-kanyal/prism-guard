package io.prismguard.analyzers.java;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Analyzer(type = "long-method")
public class LongMethodRule {

    private static final Set<String> TEST_SUFFIXES = Set.of("Test", "Tests", "Spec", "IT");

    @Check
    public List<Finding> check(RuleContext context) {
        int maxLines = context.parameter("maxLines", 50);

        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(classModel -> !isTestClass(classModel.getSimpleName()))
                .forEach(classModel -> {
                    String className = classModel.getSimpleName();
                    classModel.getMethods().forEach(method -> {
                        int lines = method.lineCount();
                        if (lines > maxLines) {
                            findings.add(Finding.builder()
                                    .ruleId("JAVA002")
                                    .ruleName("JAVA002")
                                    .severity(FindingSeverity.MEDIUM)
                                    .category(RuleCategory.JAVA_QUALITY)
                                    .message(String.format(
                                            "Method '%s.%s()' is %d lines long (max: %d)",
                                            className, method.getName(), lines, maxLines))
                                    .recommendation("Extract smaller methods. Each method should do one thing.")
                                    .filePath(classModel.getSourceFile() != null
                                            ? classModel.getSourceFile().toString()
                                            : classModel.getFullyQualifiedName())
                                    .lineNumber(method.getBeginLine())
                                    .columnNumber(0)
                                    .codeSnippet(className + "." + method.getName() + "()")
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
