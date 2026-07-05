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
 * CLEAN001 — Anemic Domain Model.
 * Domain classes that contain only accessor methods (getters/setters) with no
 * business logic are data bags, not rich domain objects. This is the Anemic
 * Domain Model anti-pattern — business rules end up scattered across services.
 */
@Analyzer(type = "anemic-domain-model")
public class AnemicDomainModelRule {

    private static final Set<String> DOMAIN_PACKAGES = Set.of(".domain.", ".model.", ".entity.");
    private static final Set<String> ACCESSOR_PREFIXES = Set.of("get", "set", "is", "has", "with");
    private static final int MIN_FIELDS = 3;
    private static final int MIN_METHODS = 2;

    @Check
    public List<Finding> check(RuleContext context) {
        int minFields = context.parameter("minFields", MIN_FIELDS);
        int minMethods = context.parameter("minMethods", MIN_METHODS);

        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.getClassType() == ClassType.CLASS)
                .filter(cls -> !cls.isSpringComponent())
                .filter(cls -> isDomainPackage(cls.getPackageName()))
                .filter(cls -> cls.fieldCount() >= minFields)
                .filter(cls -> cls.methodCount() >= minMethods)
                .forEach(cls -> {
                    long accessorCount = cls.getMethods().stream()
                            .filter(m -> ACCESSOR_PREFIXES.stream().anyMatch(prefix -> m.getName().startsWith(prefix)))
                            .count();
                    long totalPublicMethods = cls.getMethods().stream().filter(m -> m.isPublic()).count();

                    if (totalPublicMethods > 0 && accessorCount == totalPublicMethods) {
                        findings.add(Finding.builder()
                                .ruleId("CLEAN001")
                                .ruleName("CLEAN001")
                                .severity(FindingSeverity.MEDIUM)
                                .category(RuleCategory.CLEAN_ARCHITECTURE)
                                .message(String.format(
                                        "Domain class '%s' appears to be an anemic model: all %d public methods are accessors (getters/setters) with no business logic.",
                                        cls.getSimpleName(), totalPublicMethods))
                                .recommendation("Enrich the domain model by moving business logic that operates on this class's state into the class itself. Avoid 'tell, don't ask' violations where external services manipulate raw state.")
                                .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                                .lineNumber(cls.getBeginLine())
                                .columnNumber(0)
                                .codeSnippet("class " + cls.getSimpleName() + " { /* " + totalPublicMethods + " accessors, 0 business methods */ }")
                                .build());
                    }
                });

        return findings;
    }

    private boolean isDomainPackage(String packageName) {
        if (packageName == null) return false;
        return DOMAIN_PACKAGES.stream().anyMatch(packageName::contains);
    }
}
