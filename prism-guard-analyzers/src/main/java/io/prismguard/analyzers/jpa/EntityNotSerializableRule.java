package io.prismguard.analyzers.jpa;

import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.annotation.Check;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects {@code @Entity} classes that do not implement {@code java.io.Serializable}.
 * The JPA specification requires entities to be serializable when they are stored in
 * distributed caches, HTTP sessions, or passed across JVM boundaries.
 */
@Analyzer(type = "entity-not-serializable")
public class EntityNotSerializableRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.hasAnnotation("Entity"))
                .filter(cls -> cls.getImplementedInterfaces().stream()
                        .noneMatch(iface -> "Serializable".equals(iface)
                                || iface.endsWith(".Serializable")))
                .forEach(cls -> findings.add(Finding.builder()
                        .ruleId("JPA005")
                        .ruleName("Entity Not Serializable")
                        .severity(FindingSeverity.LOW)
                        .category(RuleCategory.JPA_HIBERNATE)
                        .message(String.format(
                                "Entity '%s' does not implement java.io.Serializable — "
                                        + "required for second-level caching and distributed session storage.",
                                cls.getSimpleName()))
                        .recommendation("Implement java.io.Serializable and add "
                                + "'private static final long serialVersionUID = 1L;'.")
                        .filePath(cls.getSourceFile() != null
                                ? cls.getSourceFile().toString()
                                : cls.getFullyQualifiedName())
                        .lineNumber(cls.getBeginLine())
                        .columnNumber(0)
                        .codeSnippet("@Entity class " + cls.getSimpleName())
                        .build()));

        return findings;
    }
}
