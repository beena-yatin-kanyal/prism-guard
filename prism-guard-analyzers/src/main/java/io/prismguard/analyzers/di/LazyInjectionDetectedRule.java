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
 * DI002 — @Lazy Injection Detected.
 * @Lazy on an injected field is almost always a workaround for a circular
 * dependency. It defers proxy creation to first use, which can cause confusing
 * NullPointerExceptions or unexpected initialisation order bugs.
 * The root cause (circular dependency) should be fixed instead.
 */
@Analyzer(type = "lazy-injection-detected")
public class LazyInjectionDetectedRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.isSpringComponent())
                .forEach(cls -> {
                    cls.getFields().stream()
                            .filter(f -> f.hasAnnotation("Lazy")
                                    || f.hasAnnotation("org.springframework.context.annotation.Lazy"))
                            .filter(f -> f.isInjected())
                            .forEach(f -> findings.add(Finding.builder()
                                    .ruleId("DI002")
                                    .ruleName("DI002")
                                    .severity(FindingSeverity.HIGH)
                                    .category(RuleCategory.DEPENDENCY_INJECTION)
                                    .message(String.format(
                                            "Class '%s' uses @Lazy injection on field '%s'. This pattern is typically a workaround for a circular dependency and defers failure to first use.",
                                            cls.getSimpleName(), f.name()))
                                    .recommendation("Resolve the underlying circular dependency by breaking the cycle: introduce an interface, use an event-driven approach, or extract the shared logic into a third bean that neither participant depends on. Avoid using @Lazy as a shortcut.")
                                    .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                                    .lineNumber(cls.getBeginLine())
                                    .columnNumber(0)
                                    .codeSnippet("@Lazy @Autowired " + f.type() + " " + f.name())
                                    .build()));
                });

        return findings;
    }
}
