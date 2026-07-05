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
 * JAVA013 — Test Class Without Tests.
 * A class whose name indicates it is a test class but contains no @Test methods
 * provides no coverage value and may mislead developers about test completeness.
 */
@Analyzer(type = "test-class-no-tests")
public class TestClassWithoutTestsRule {

    private static final Set<String> TEST_SUFFIXES = Set.of("Test", "Tests", "Spec", "IT");
    private static final Set<String> TEST_ANNOTATIONS = Set.of(
            "Test", "ParameterizedTest", "RepeatedTest",
            "org.junit.Test", "org.junit.jupiter.api.Test",
            "org.junit.jupiter.params.ParameterizedTest"
    );

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> isTestClass(cls.getSimpleName()))
                .forEach(cls -> {
                    boolean hasTestMethod = cls.getMethods().stream()
                            .anyMatch(m -> m.getAnnotations().stream()
                                    .anyMatch(a -> TEST_ANNOTATIONS.stream()
                                            .anyMatch(ta -> a.equals(ta) || a.endsWith("." + ta))));
                    if (!hasTestMethod) {
                        findings.add(Finding.builder()
                                .ruleId("JAVA013")
                                .ruleName("JAVA013")
                                .severity(FindingSeverity.LOW)
                                .category(RuleCategory.JAVA_QUALITY)
                                .message(String.format(
                                        "Test class '%s' contains no @Test methods. It provides no test coverage.",
                                        cls.getSimpleName()))
                                .recommendation("Add @Test methods to provide actual coverage, or remove the class if it is unused. Empty test classes can create a false sense of test completeness in coverage reports.")
                                .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                                .lineNumber(cls.getBeginLine())
                                .columnNumber(0)
                                .codeSnippet("class " + cls.getSimpleName() + " { /* no @Test methods */ }")
                                .build());
                    }
                });

        return findings;
    }

    private boolean isTestClass(String simpleName) {
        return TEST_SUFFIXES.stream().anyMatch(simpleName::endsWith);
    }
}
