package io.prismguard.core.engine;

import io.prismguard.api.config.PrismGuardConfiguration;
import io.prismguard.api.config.RuleConfig;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Shared utilities for post-processing {@link Finding}s with config-sourced metadata.
 *
 * <p>Used exclusively by {@link AnalyzerBasedRule}.
 */
final class AnalyzerSupport {

    private static final Logger log = LoggerFactory.getLogger(AnalyzerSupport.class);

    private AnalyzerSupport() {
    }

    /**
     * Resolves the effective severity in priority order:
     * top-level {@code ruleSeverityOverrides} → per-rule {@code severity} → {@code MEDIUM}.
     */
    static FindingSeverity resolveEffectiveSeverity(String ruleId, RuleConfig rc,
                                                    PrismGuardConfiguration config) {
        FindingSeverity topLevel = config.getRuleSeverityOverrides().get(ruleId);
        if (topLevel != null) return topLevel;
        if (rc.getSeverity() != null) return rc.getSeverity();
        return FindingSeverity.MEDIUM;
    }

    /**
     * Parses a category name from YAML (e.g. {@code "JAVA_QUALITY"} or {@code "java-quality"})
     * into a {@link RuleCategory}, falling back to {@code JAVA_QUALITY} on unknown values.
     */
    static RuleCategory parseCategory(String ruleId, String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return RuleCategory.JAVA_QUALITY;
        }
        String normalized = categoryName.toUpperCase().replace('-', '_').replace(' ', '_');
        try {
            return RuleCategory.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            log.warn("Rule '{}': unknown category '{}' in prismguard.yml, defaulting to JAVA_QUALITY",
                    ruleId, categoryName);
            return RuleCategory.JAVA_QUALITY;
        }
    }

    /**
     * Post-processes findings by stamping each with YAML-driven metadata read directly
     * from the supplied {@link RuleConfig}.
     *
     * <p>Analyzer classes produce findings with only location/message data. This method
     * overlays the canonical {@code ruleId}, {@code ruleName}, {@code severity}, and
     * {@code category} from the resolved config. If the YAML provides a
     * {@code recommendation}, it overrides the analyzer's own text.
     */
    static List<Finding> resolveFromConfig(String ruleId, List<Finding> findings,
                                           RuleConfig ruleConfig,
                                           PrismGuardConfiguration config) {
        if (findings.isEmpty()) return findings;

        final String finalRuleId       = ruleConfig.getId() != null ? ruleConfig.getId() : ruleId;
        final String finalRuleName     = ruleConfig.getTitle() != null ? ruleConfig.getTitle() : ruleId;
        final FindingSeverity finalSev = resolveEffectiveSeverity(ruleId, ruleConfig, config);
        final RuleCategory finalCat    = parseCategory(ruleId, ruleConfig.getCategory());
        final String yamlRecommendation = ruleConfig.getRecommendation();

        return findings.stream().map(f -> Finding.builder()
                .ruleId(finalRuleId)
                .ruleName(finalRuleName)
                .severity(finalSev)
                .category(finalCat)
                .message(f.getMessage())
                .recommendation(yamlRecommendation != null ? yamlRecommendation : f.getRecommendation())
                .filePath(f.getFilePath())
                .lineNumber(f.getLineNumber())
                .columnNumber(f.getColumnNumber())
                .codeSnippet(f.getCodeSnippet())
                .build()
        ).collect(Collectors.toList());
    }

    /**
     * Coerces the return value of a {@link io.prismguard.api.annotation.Check} method
     * into a {@code List<Finding>}.
     * Supported types: {@code null}, {@link Finding}, {@code List<Finding>},
     * {@code Optional<Finding>}.
     */
    @SuppressWarnings("unchecked")
    static List<Finding> coerceToFindings(Object result, String classNameForError) {
        if (result == null)                    return List.of();
        if (result instanceof Finding f)       return List.of(f);
        if (result instanceof List<?> list)    return (List<Finding>) list;
        if (result instanceof Optional<?> opt) return opt.map(v -> List.of((Finding) v)).orElse(List.of());
        throw new IllegalStateException(
                "@Check method on " + classNameForError
                        + " must return Finding, List<Finding>, Optional<Finding>, or null — got: "
                        + result.getClass().getName());
    }
}
