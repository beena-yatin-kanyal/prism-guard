package io.prismguard.analyzers.spring;

import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.annotation.Check;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;

/**
 * SPRING014 — @ConfigurationProperties Without @Validated.
 * Configuration properties classes without @Validated will not have their
 * constraints evaluated, allowing the application to start with invalid config.
 */
@Analyzer(type = "configuration-properties-no-validation")
public class ConfigurationPropertiesNoValidationRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.hasAnnotation("ConfigurationProperties"))
                .forEach(cls -> {
                    boolean hasValidated = cls.hasAnnotation("Validated")
                            || cls.hasAnnotation("org.springframework.validation.annotation.Validated");
                    if (!hasValidated) {
                        findings.add(Finding.builder()
                                .ruleId("SPRING014")
                                .ruleName("SPRING014")
                                .severity(FindingSeverity.MEDIUM)
                                .category(RuleCategory.CONFIGURATION)
                                .message(String.format(
                                        "Configuration properties class '%s' is annotated with @ConfigurationProperties but not with @Validated. Bean Validation constraints on its fields will be silently ignored.",
                                        cls.getSimpleName()))
                                .recommendation("Add @Validated to the class so that Spring validates constraint annotations (e.g., @NotNull, @Min, @Pattern) at startup and fails fast on invalid configuration rather than at runtime.")
                                .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                                .lineNumber(cls.getBeginLine())
                                .columnNumber(0)
                                .codeSnippet("@ConfigurationProperties class " + cls.getSimpleName() + " { /* missing @Validated */ }")
                                .build());
                    }
                });

        return findings;
    }
}
