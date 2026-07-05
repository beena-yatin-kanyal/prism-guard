package io.prismguard.analyzers.cleanarch;

import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.annotation.Check;
import io.prismguard.api.model.ClassType;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * CLEAN003 — Service Without Interface.
 * @Service classes that implement no business interface are tightly coupled to
 * their callers. This prevents swapping implementations, hinders testability
 * (no interface to mock), and violates the Dependency Inversion Principle.
 */
@Analyzer(type = "service-without-interface")
public class ServiceWithoutInterfaceRule {

    private static final Set<String> IGNORED_INTERFACES = Set.of(
            "Serializable", "Cloneable", "Comparable", "AutoCloseable", "Closeable",
            "ApplicationListener", "InitializingBean", "DisposableBean", "SmartLifecycle",
            "CommandLineRunner", "ApplicationRunner"
    );

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.getClassType() == ClassType.CLASS)
                .filter(cls -> cls.hasAnnotation("Service"))
                .forEach(cls -> {
                    List<String> businessInterfaces = cls.getImplementedInterfaces().stream()
                            .filter(i -> !IGNORED_INTERFACES.contains(simpleName(i)))
                            .toList();

                    if (businessInterfaces.isEmpty()) {
                        findings.add(Finding.builder()
                                .ruleId("CLEAN003")
                                .ruleName("CLEAN003")
                                .severity(FindingSeverity.MEDIUM)
                                .category(RuleCategory.CLEAN_ARCHITECTURE)
                                .message(String.format(
                                        "Service class '%s' does not implement any business interface. Callers are directly coupled to the concrete implementation.",
                                        cls.getSimpleName()))
                                .recommendation(String.format(
                                        "Extract a business interface (e.g., '%sPort' or '%sService') and have the @Service implement it. Inject the interface in controllers and other services to decouple callers from the implementation and make it mockable in tests.",
                                        cls.getSimpleName().replaceAll("Impl$", ""), cls.getSimpleName().replaceAll("Impl$", "")))
                                .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                                .lineNumber(cls.getBeginLine())
                                .columnNumber(0)
                                .codeSnippet("@Service class " + cls.getSimpleName() + " { /* no interface */ }")
                                .build());
                    }
                });

        return findings;
    }

    private String simpleName(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot >= 0 ? fqn.substring(dot + 1) : fqn;
    }
}
