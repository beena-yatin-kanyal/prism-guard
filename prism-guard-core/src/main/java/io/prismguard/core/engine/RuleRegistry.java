package io.prismguard.core.engine;

import io.prismguard.api.config.PrismGuardConfiguration;
import io.prismguard.api.config.RuleConfig;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Resolves analysis rules from the project's {@code prismguard.yml} configuration.
 *
 * <p>There is no static rule catalog. The concept of "available rules" only exists in
 * the context of a specific {@code prismguard.yml} — therefore all methods require a
 * {@link PrismGuardConfiguration}. Rules are defined in YAML via the {@code analyzer:}
 * field, which references an {@link io.prismguard.api.annotation.Analyzer} type
 * discovered at startup by {@link AnalyzerRegistry}.
 *
 * <p>External plugins contribute rules by placing {@code @Analyzer}-annotated classes
 * and a {@code META-INF/prismguard/analyzers} service file in a JAR on the classpath.
 * No explicit registration is required.
 */
@Component
public class RuleRegistry {

    private static final Logger log = LoggerFactory.getLogger(RuleRegistry.class);

    private final AnalyzerRegistry analyzerRegistry;

    public RuleRegistry(AnalyzerRegistry analyzerRegistry) {
        this.analyzerRegistry = analyzerRegistry;
        log.info("RuleRegistry initialised ({} analyzer type(s) available)", analyzerRegistry.size());
    }

    /**
     * Returns all rules enabled by the given configuration.
     *
     * <p>A rule entry in the YAML is included when all of the following hold:
     * <ol>
     *   <li>its ID passes the {@code disabledRules} / {@code enabled} checks</li>
     *   <li>its category is in {@code enabledCategories} (when the list is non-empty)</li>
     *   <li>its {@code analyzer:} field references a known analyzer type</li>
     * </ol>
     */
    public List<Rule> getEnabledRules(PrismGuardConfiguration config) {
        return config.getRuleConfigs().entrySet().stream()
                .filter(e -> config.isRuleEnabled(e.getKey()))
                .filter(e -> isCategoryEnabled(e.getValue(), config))
                .filter(e -> e.getValue().getAnalyzer() != null && !e.getValue().getAnalyzer().isBlank())
                .flatMap(e -> toRule(e.getKey(), e.getValue(), config).stream())
                .toList();
    }

    /**
     * Finds a rule by ID from the given configuration, or empty if the rule
     * has no {@code analyzer:} field or the analyzer type is unknown.
     */
    public Optional<Rule> findById(String ruleId, PrismGuardConfiguration config) {
        RuleConfig rc = config.getRuleConfig(ruleId);
        if (rc.getAnalyzer() == null || rc.getAnalyzer().isBlank()) return Optional.empty();
        return toRule(ruleId, rc, config);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private boolean isCategoryEnabled(RuleConfig rc, PrismGuardConfiguration config) {
        if (rc.getCategory() == null || rc.getCategory().isBlank()) return true;
        String normalized = rc.getCategory().toUpperCase().replace('-', '_').replace(' ', '_');
        try {
            RuleCategory cat = RuleCategory.valueOf(normalized);
            return config.isCategoryEnabled(cat.name()) || config.isCategoryEnabled(cat.getShortCode());
        } catch (IllegalArgumentException e) {
            return true; // unknown category — validator will have warned; don't silently drop the rule
        }
    }

    private Optional<Rule> toRule(String ruleId, RuleConfig rc, PrismGuardConfiguration config) {
        Optional<AnalyzerInstance> instance = analyzerRegistry.findByType(rc.getAnalyzer());
        if (instance.isEmpty()) {
            log.warn("Rule '{}' references unknown analyzer type '{}' — skipping", ruleId, rc.getAnalyzer());
            return Optional.empty();
        }
        return Optional.of(new AnalyzerBasedRule(instance.get(), ruleId, config));
    }
}
