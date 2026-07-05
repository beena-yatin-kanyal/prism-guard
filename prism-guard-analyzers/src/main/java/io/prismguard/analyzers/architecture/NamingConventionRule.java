package io.prismguard.analyzers.architecture;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.ClassModel;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * ARCH006 — Naming Convention Violation.
 *
 * Enforces naming conventions for Spring stereotype annotations:
 * <ul>
 *   <li>@Service → name must end in {@code Service} or {@code ServiceImpl}</li>
 *   <li>@Repository → name must end in {@code Repository}, {@code RepositoryImpl}, {@code Dao}, or {@code DaoImpl}</li>
 *   <li>@Controller / @RestController → name must end in {@code Controller}</li>
 *   <li>@Configuration → name must end in {@code Config} or {@code Configuration}</li>
 *   <li>@Entity → name must NOT end in {@code Service}, {@code Controller}, or {@code Repository}</li>
 * </ul>
 */
@Analyzer(type = "naming-convention")
public class NamingConventionRule {

    private static final Logger log = LoggerFactory.getLogger(NamingConventionRule.class);

    /**
     * Encapsulates a naming convention check for a given annotation.
     */
    private record ConventionCheck(
            String annotation,
            List<String> allowedSuffixes,
            boolean requiresSuffix
    ) {
        /** Returns true if the class name satisfies this convention. */
        boolean satisfiedBy(String simpleName) {
            boolean hasSuffix = allowedSuffixes.stream().anyMatch(simpleName::endsWith);
            return requiresSuffix == hasSuffix;
        }

        String expectedSuffixDescription() {
            return String.join(" or ", allowedSuffixes);
        }
    }

    /** Suffixes that domain entities must never end in. */
    private static final List<String> ENTITY_FORBIDDEN_SUFFIXES = List.of(
            "Service", "Controller", "Repository"
    );

    private static final List<ConventionCheck> CONVENTION_CHECKS = List.of(
            new ConventionCheck("Service",
                    List.of("Service", "ServiceImpl"),
                    true),
            new ConventionCheck("Repository",
                    List.of("Repository", "RepositoryImpl", "Dao", "DaoImpl"),
                    true),
            new ConventionCheck("Controller",
                    List.of("Controller"),
                    true),
            new ConventionCheck("RestController",
                    List.of("Controller"),
                    true),
            new ConventionCheck("Configuration",
                    List.of("Config", "Configuration"),
                    true)
    );

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses().forEach(cls -> analyzeClass(cls, findings));

        return findings;
    }

    private void analyzeClass(ClassModel cls, List<Finding> findings) {
        String simpleName = cls.getSimpleName();

        // Check stereotype annotation conventions
        for (ConventionCheck check : CONVENTION_CHECKS) {
            if (!cls.hasAnnotation(check.annotation())) {
                continue;
            }
            if (!check.satisfiedBy(simpleName)) {
                findings.add(Finding.builder()
                        .ruleId("ARCH006")
                        .ruleName("ARCH006")
                        .severity(FindingSeverity.LOW)
                        .category(RuleCategory.ARCHITECTURE)
                        .message(String.format(
                                "Class '%s' is annotated @%s but does not follow naming convention (should end in '%s').",
                                simpleName, check.annotation(), check.expectedSuffixDescription()))
                        .recommendation("Follow consistent naming conventions for Spring stereotypes to improve codebase navigability.")
                        .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : "")
                        .lineNumber(cls.getBeginLine())
                        .columnNumber(0)
                        .codeSnippet(String.format("@%s class %s {}", check.annotation(), simpleName))
                        .build());
            }
        }

        // Check @Entity does not have infrastructure-style names
        if (cls.hasAnnotation("Entity")
                || cls.hasAnnotation("javax.persistence.Entity")
                || cls.hasAnnotation("jakarta.persistence.Entity")) {
            for (String forbidden : ENTITY_FORBIDDEN_SUFFIXES) {
                if (simpleName.endsWith(forbidden)) {
                    findings.add(Finding.builder()
                            .ruleId("ARCH006")
                            .ruleName("ARCH006")
                            .severity(FindingSeverity.LOW)
                            .category(RuleCategory.ARCHITECTURE)
                            .message(String.format(
                                    "Class '%s' is annotated @Entity but its name ends in '%s', which conflicts with stereotype naming conventions.",
                                    simpleName, forbidden))
                            .recommendation("Rename the entity class to reflect its domain concept, not an architectural role.")
                            .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : "")
                            .lineNumber(cls.getBeginLine())
                            .columnNumber(0)
                            .codeSnippet(String.format("@Entity class %s {}", simpleName))
                            .build());
                    break; // Report once per entity
                }
            }
        }
    }
}
