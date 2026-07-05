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
 * Detects {@code @Entity} classes whose {@code @Id} field lacks {@code @GeneratedValue}.
 * Without a generation strategy the application must assign primary keys manually,
 * which is error-prone and easily forgotten.
 */
@Analyzer(type = "entity-missing-generated-value")
public class EntityMissingGeneratedValueRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.hasAnnotation("Entity"))
                .forEach(cls -> cls.getFields().stream()
                        .filter(f -> f.hasAnnotation("Id") && !f.hasAnnotation("EmbeddedId"))
                        .filter(f -> !f.hasAnnotation("GeneratedValue"))
                        .forEach(f -> findings.add(Finding.builder()
                                .ruleId("JPA001")
                                .ruleName("Entity Missing @GeneratedValue")
                                .severity(FindingSeverity.HIGH)
                                .category(RuleCategory.JPA_HIBERNATE)
                                .message(String.format(
                                        "Entity '%s' has @Id field '%s' without @GeneratedValue — "
                                                + "the application must assign primary keys manually.",
                                        cls.getSimpleName(), f.name()))
                                .recommendation("Add @GeneratedValue(strategy = GenerationType.IDENTITY) "
                                        + "or another strategy suited to your database.")
                                .filePath(cls.getSourceFile() != null
                                        ? cls.getSourceFile().toString()
                                        : cls.getFullyQualifiedName())
                                .lineNumber(cls.getBeginLine())
                                .columnNumber(0)
                                .codeSnippet("@Id " + f.type() + " " + f.name())
                                .build())));

        return findings;
    }
}
