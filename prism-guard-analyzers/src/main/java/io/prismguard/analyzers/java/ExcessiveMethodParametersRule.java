package io.prismguard.analyzers.java;

import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.annotation.Check;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * JAVA011 — Excessive Method Parameters.
 * Methods with too many parameters are hard to call, read, and test.
 * They often indicate that a parameter object should be introduced.
 */
@Analyzer(type = "excessive-method-parameters")
public class ExcessiveMethodParametersRule {

    private static final Set<String> TEST_SUFFIXES = Set.of("Test", "Tests", "Spec", "IT");

    @Check
    public List<Finding> check(RuleContext context) {
        int maxParams = context.parameter("maxParams", 7);

        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> !isTestClass(cls.getSimpleName()))
                .forEach(cls -> {
                    String className = cls.getSimpleName();
                    cls.getMethods().forEach(method -> {
                        int paramCount = method.getParameterTypes().size();
                        if (paramCount > maxParams) {
                            findings.add(Finding.builder()
                                    .ruleId("JAVA011")
                                    .ruleName("JAVA011")
                                    .severity(FindingSeverity.MEDIUM)
                                    .category(RuleCategory.JAVA_QUALITY)
                                    .message(String.format(
                                            "Method '%s.%s()' has %d parameters (max: %d). Consider introducing a parameter object.",
                                            className, method.getName(), paramCount, maxParams))
                                    .recommendation("Introduce a dedicated parameter object (value object or record) to group related parameters. This improves readability, reduces coupling, and makes future changes cheaper.")
                                    .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                                    .lineNumber(method.getBeginLine())
                                    .columnNumber(0)
                                    .codeSnippet(className + "." + method.getName() + "(" + paramCount + " params)")
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
