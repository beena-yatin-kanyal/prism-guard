package io.prismguard.analyzers.cleanarch;

import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.annotation.Check;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.JavaFileModel;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * CLEAN002 — Domain Layer Depends on Framework.
 * Pure domain classes should have no knowledge of persistence or web frameworks.
 * Importing Spring, JPA, or Servlet classes in a domain package couples the
 * domain model to infrastructure, making it impossible to run outside a container.
 */
@Analyzer(type = "domain-depends-on-framework")
public class DomainDependsOnFrameworkRule {

    private static final Set<String> DOMAIN_PACKAGES = Set.of(".domain.", ".model.");

    private static final Set<String> FORBIDDEN_IMPORT_PREFIXES = Set.of(
            "org.springframework.",
            "javax.persistence.",
            "jakarta.persistence.",
            "javax.servlet.",
            "jakarta.servlet.",
            "org.hibernate."
    );

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allFiles()
                .filter(file -> isDomainPackage(file.getPackageName()))
                .forEach(file -> {
                    file.getImports().forEach(imp -> {
                        FORBIDDEN_IMPORT_PREFIXES.stream()
                                .filter(imp::startsWith)
                                .findFirst()
                                .ifPresent(prefix -> {
                                    String primaryClass = file.getClasses().isEmpty()
                                            ? file.getPackageName()
                                            : file.getClasses().get(0).getSimpleName();
                                    findings.add(Finding.builder()
                                            .ruleId("CLEAN002")
                                            .ruleName("CLEAN002")
                                            .severity(FindingSeverity.HIGH)
                                            .category(RuleCategory.CLEAN_ARCHITECTURE)
                                            .message(String.format(
                                                    "Domain class '%s' imports framework class '%s'. Domain objects must not depend on infrastructure or framework details.",
                                                    primaryClass, imp))
                                            .recommendation("Remove framework imports from domain classes. Use domain-neutral interfaces (e.g., repository interfaces in the domain package with implementations in the infrastructure layer) so the domain can be tested and run independently of any framework.")
                                            .filePath(file.getFilePath() != null ? file.getFilePath().toString() : "")
                                            .lineNumber(0)
                                            .columnNumber(0)
                                            .codeSnippet("import " + imp + ";")
                                            .build());
                                });
                    });
                });

        return findings;
    }

    private boolean isDomainPackage(String packageName) {
        if (packageName == null) return false;
        return DOMAIN_PACKAGES.stream().anyMatch(packageName::contains);
    }
}
