package io.prismguard.analyzers.di;

import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.annotation.Check;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;

/**
 * DI003 — Optional Dependency Injection.
 * Injecting Optional<T> as a dependency makes the dependency graph ambiguous.
 * If a dependency may or may not be present, that optionality should be expressed
 * by providing a no-op default implementation or a conditional bean, not by
 * wrapping the injection point in Optional.
 */
@Analyzer(type = "optional-dependency-injection")
public class OptionalDependencyInjectionRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.isSpringComponent())
                .forEach(cls -> {
                    cls.getFields().stream()
                            .filter(f -> f.isInjected())
                            .filter(f -> f.type() != null && f.type().startsWith("Optional"))
                            .forEach(f -> findings.add(Finding.builder()
                                    .ruleId("DI003")
                                    .ruleName("DI003")
                                    .severity(FindingSeverity.MEDIUM)
                                    .category(RuleCategory.DEPENDENCY_INJECTION)
                                    .message(String.format(
                                            "Class '%s' injects '%s' as Optional. Optional injection makes the dependency contract unclear and leads to scattered null-checks throughout the class.",
                                            cls.getSimpleName(), f.type()))
                                    .recommendation("Provide a default no-op implementation using @ConditionalOnMissingBean, or use @Autowired(required = false) with explicit null-handling in one place. Alternatively, define a @Bean that always provides a valid (possibly no-op) implementation so the injection is always satisfied.")
                                    .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                                    .lineNumber(cls.getBeginLine())
                                    .columnNumber(0)
                                    .codeSnippet("@Autowired " + f.type() + " " + f.name())
                                    .build()));
                });

        return findings;
    }
}
