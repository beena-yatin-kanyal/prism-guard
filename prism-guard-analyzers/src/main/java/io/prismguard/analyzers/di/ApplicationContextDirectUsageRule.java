package io.prismguard.analyzers.di;

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
 * DI001 — Direct ApplicationContext Usage (Service Locator Anti-Pattern).
 * Injecting ApplicationContext or BeanFactory to look up beans at runtime is
 * the Service Locator anti-pattern. It hides dependencies, couples code to
 * Spring's internals, and makes unit testing without a running context impossible.
 */
@Analyzer(type = "application-context-direct-usage")
public class ApplicationContextDirectUsageRule {

    private static final Set<String> CONTEXT_TYPES = Set.of(
            "ApplicationContext",
            "ConfigurableApplicationContext",
            "BeanFactory",
            "ListableBeanFactory",
            "AutowireCapableBeanFactory",
            "org.springframework.context.ApplicationContext",
            "org.springframework.context.ConfigurableApplicationContext",
            "org.springframework.beans.factory.BeanFactory",
            "org.springframework.beans.factory.ListableBeanFactory"
    );

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.isSpringComponent())
                .forEach(cls -> {
                    cls.getFields().stream()
                            .filter(f -> isContextType(f.type()))
                            .forEach(f -> findings.add(Finding.builder()
                                    .ruleId("DI001")
                                    .ruleName("DI001")
                                    .severity(FindingSeverity.HIGH)
                                    .category(RuleCategory.DEPENDENCY_INJECTION)
                                    .message(String.format(
                                            "Class '%s' injects '%s' directly. This is the Service Locator anti-pattern — it hides real dependencies and couples code to the Spring container.",
                                            cls.getSimpleName(), f.type()))
                                    .recommendation("Declare the exact beans you need as constructor parameters instead of injecting ApplicationContext and calling getBean(). This makes dependencies explicit, allows constructor-based testing without a Spring context, and improves testability.")
                                    .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                                    .lineNumber(cls.getBeginLine())
                                    .columnNumber(0)
                                    .codeSnippet("@Autowired " + f.type() + " " + f.name() + "; // service locator")
                                    .build()));
                });

        return findings;
    }

    private boolean isContextType(String type) {
        return CONTEXT_TYPES.stream().anyMatch(ct ->
                type.equals(ct) || type.startsWith(ct + "<"));
    }
}
