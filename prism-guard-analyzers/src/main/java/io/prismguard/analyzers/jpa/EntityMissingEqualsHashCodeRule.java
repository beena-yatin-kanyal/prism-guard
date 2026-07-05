package io.prismguard.analyzers.jpa;

import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.annotation.Check;
import io.prismguard.api.model.ClassType;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects {@code @Entity} classes that do not override {@code equals()} and {@code hashCode()}.
 * Without these overrides, Hibernate collections (e.g. Sets) use identity equality, which
 * causes subtle bugs when merging detached entities or working with second-level caches.
 */
@Analyzer(type = "entity-missing-equals-hashcode")
public class EntityMissingEqualsHashCodeRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.hasAnnotation("Entity"))
                .filter(cls -> cls.getClassType() != ClassType.RECORD) // records auto-generate
                .forEach(cls -> {
                    boolean hasEquals = cls.getMethods().stream()
                            .anyMatch(m -> "equals".equals(m.getName())
                                    && m.getParameterTypes().size() == 1);
                    boolean hasHashCode = cls.getMethods().stream()
                            .anyMatch(m -> "hashCode".equals(m.getName())
                                    && m.getParameterTypes().isEmpty());

                    if (!hasEquals || !hasHashCode) {
                        String missing = (!hasEquals && !hasHashCode)
                                ? "equals() and hashCode()"
                                : (!hasEquals ? "equals()" : "hashCode()");
                        findings.add(Finding.builder()
                                .ruleId("JPA002")
                                .ruleName("Entity Missing equals/hashCode")
                                .severity(FindingSeverity.HIGH)
                                .category(RuleCategory.JPA_HIBERNATE)
                                .message(String.format(
                                        "Entity '%s' does not override %s — identity in Sets and Maps "
                                                + "will be based on object reference, not primary key.",
                                        cls.getSimpleName(), missing))
                                .recommendation("Override equals() and hashCode() using the entity's "
                                        + "primary key, or use a business-key if the PK is not yet assigned.")
                                .filePath(cls.getSourceFile() != null
                                        ? cls.getSourceFile().toString()
                                        : cls.getFullyQualifiedName())
                                .lineNumber(cls.getBeginLine())
                                .columnNumber(0)
                                .codeSnippet("@Entity class " + cls.getSimpleName())
                                .build());
                    }
                });

        return findings;
    }
}
