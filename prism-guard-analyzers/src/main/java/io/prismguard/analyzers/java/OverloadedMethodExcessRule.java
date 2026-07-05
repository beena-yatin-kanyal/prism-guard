package io.prismguard.analyzers.java;

import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.annotation.Check;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JAVA015 — Excessive Method Overloading.
 * Having many overloads of the same method name inflates the API surface,
 * makes the class harder to understand, and is often a sign that a builder
 * or fluent configuration API should be used instead.
 */
@Analyzer(type = "overloaded-method-excess")
public class OverloadedMethodExcessRule {

    private static final Set<String> TEST_SUFFIXES = Set.of("Test", "Tests", "Spec", "IT");

    @Check
    public List<Finding> check(RuleContext context) {
        int maxOverloads = context.parameter("maxOverloads", 4);

        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> !isTestClass(cls.getSimpleName()))
                .forEach(cls -> {
                    Map<String, Long> overloadCounts = cls.getMethods().stream()
                            .collect(Collectors.groupingBy(m -> m.getName(), Collectors.counting()));

                    overloadCounts.entrySet().stream()
                            .filter(e -> e.getValue() > maxOverloads)
                            .forEach(e -> findings.add(Finding.builder()
                                    .ruleId("JAVA015")
                                    .ruleName("JAVA015")
                                    .severity(FindingSeverity.LOW)
                                    .category(RuleCategory.MAINTAINABILITY)
                                    .message(String.format(
                                            "Method '%s' in class '%s' has %d overloads (max: %d). Excessive overloading inflates the public API and makes the class harder to understand.",
                                            e.getKey(), cls.getSimpleName(), e.getValue(), maxOverloads))
                                    .recommendation("Consider replacing multiple overloads with a builder pattern, optional parameters via a configuration object, or varargs where appropriate.")
                                    .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                                    .lineNumber(cls.getBeginLine())
                                    .columnNumber(0)
                                    .codeSnippet(cls.getSimpleName() + "." + e.getKey() + "(...) — " + e.getValue() + " overloads")
                                    .build()));
                });

        return findings;
    }

    private boolean isTestClass(String simpleName) {
        return TEST_SUFFIXES.stream().anyMatch(simpleName::endsWith);
    }
}
