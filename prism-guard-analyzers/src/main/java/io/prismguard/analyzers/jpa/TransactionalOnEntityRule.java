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
 * Detects {@code @Entity} classes that are also annotated with {@code @Transactional}.
 * JPA entities are not Spring-managed beans; {@code @Transactional} on an entity is
 * silently ignored because Spring AOP cannot intercept direct instantiation.
 */
@Analyzer(type = "transactional-on-entity")
public class TransactionalOnEntityRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.hasAnnotation("Entity"))
                .filter(cls -> cls.hasAnnotation("Transactional"))
                .forEach(cls -> findings.add(Finding.builder()
                        .ruleId("JPA004")
                        .ruleName("@Transactional on Entity")
                        .severity(FindingSeverity.HIGH)
                        .category(RuleCategory.TRANSACTION)
                        .message(String.format(
                                "Entity '%s' is annotated with @Transactional — "
                                        + "JPA entities are not Spring beans, so the annotation has no effect.",
                                cls.getSimpleName()))
                        .recommendation("Remove @Transactional from the entity. "
                                + "Place transaction boundaries on @Service or @Repository classes "
                                + "that manage the entity's lifecycle.")
                        .filePath(cls.getSourceFile() != null
                                ? cls.getSourceFile().toString()
                                : cls.getFullyQualifiedName())
                        .lineNumber(cls.getBeginLine())
                        .columnNumber(0)
                        .codeSnippet("@Transactional @Entity class " + cls.getSimpleName())
                        .build()));

        return findings;
    }
}
