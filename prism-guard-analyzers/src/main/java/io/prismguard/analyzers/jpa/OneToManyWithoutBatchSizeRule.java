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
 * Detects {@code @OneToMany} or {@code @ManyToMany} collection fields on {@code @Entity} classes
 * that are not annotated with {@code @BatchSize} or {@code @Fetch}.
 * Without a batch-fetching strategy every lazy collection triggers a separate SELECT,
 * leading to the classic N+1 query problem.
 */
@Analyzer(type = "one-to-many-without-batch-size")
public class OneToManyWithoutBatchSizeRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.hasAnnotation("Entity"))
                .forEach(cls -> cls.getFields().stream()
                        .filter(f -> f.hasAnnotation("OneToMany") || f.hasAnnotation("ManyToMany"))
                        .filter(f -> !f.hasAnnotation("BatchSize") && !f.hasAnnotation("Fetch"))
                        .forEach(f -> {
                            String relation = f.hasAnnotation("OneToMany") ? "@OneToMany" : "@ManyToMany";
                            findings.add(Finding.builder()
                                    .ruleId("JPA006")
                                    .ruleName("Collection Without Batch-Fetch Strategy")
                                    .severity(FindingSeverity.MEDIUM)
                                    .category(RuleCategory.PERFORMANCE)
                                    .message(String.format(
                                            "Entity '%s' field '%s' uses %s without @BatchSize or @Fetch — "
                                                    + "each lazy load issues a separate SQL query (N+1 risk).",
                                            cls.getSimpleName(), f.name(), relation))
                                    .recommendation("Add @BatchSize(size = 20) on the field, or use "
                                            + "@Fetch(FetchMode.SUBSELECT) to batch-load the entire collection in one query.")
                                    .filePath(cls.getSourceFile() != null
                                            ? cls.getSourceFile().toString()
                                            : cls.getFullyQualifiedName())
                                    .lineNumber(cls.getBeginLine())
                                    .columnNumber(0)
                                    .codeSnippet(relation + " " + f.type() + " " + f.name())
                                    .build());
                        }));

        return findings;
    }
}
