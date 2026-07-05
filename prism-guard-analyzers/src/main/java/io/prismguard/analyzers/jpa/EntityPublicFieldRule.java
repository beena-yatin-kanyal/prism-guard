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
 * Detects {@code @Entity} classes with non-private, non-static fields.
 * Hibernate uses either field-based or property-based access depending on where the
 * {@code @Id} annotation is placed. Public fields break encapsulation and interfere
 * with Hibernate's proxy-subclassing mechanism.
 */
@Analyzer(type = "entity-public-field")
public class EntityPublicFieldRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.hasAnnotation("Entity"))
                .forEach(cls -> cls.getFields().stream()
                        .filter(f -> !f.isPrivate() && !f.isStatic() && !f.isFinal())
                        .forEach(f -> findings.add(Finding.builder()
                                .ruleId("JPA003")
                                .ruleName("Public Field in Entity")
                                .severity(FindingSeverity.MEDIUM)
                                .category(RuleCategory.JPA_HIBERNATE)
                                .message(String.format(
                                        "Entity '%s' has non-private field '%s' — "
                                                + "public fields bypass Hibernate's proxy and dirty-checking mechanisms.",
                                        cls.getSimpleName(), f.name()))
                                .recommendation("Make the field private. Provide getters/setters if "
                                        + "external access is needed.")
                                .filePath(cls.getSourceFile() != null
                                        ? cls.getSourceFile().toString()
                                        : cls.getFullyQualifiedName())
                                .lineNumber(cls.getBeginLine())
                                .columnNumber(0)
                                .codeSnippet(f.type() + " " + f.name())
                                .build())));

        return findings;
    }
}
