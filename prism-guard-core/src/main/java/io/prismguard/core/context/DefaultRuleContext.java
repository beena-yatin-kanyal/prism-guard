package io.prismguard.core.context;

import io.prismguard.api.config.PrismGuardConfiguration;
import io.prismguard.api.config.RuleConfig;
import io.prismguard.api.model.ProjectModel;
import io.prismguard.api.rule.RuleContext;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Canonical implementation of {@link RuleContext}.
 *
 * <p>Two construction modes:
 * <ul>
 *   <li><b>Analysis-level</b>: {@link #DefaultRuleContext(ProjectModel, PrismGuardConfiguration)} —
 *       the standard context passed to every rule. {@link #parameter} returns
 *       {@code defaultValue} because no rule-specific config is loaded.</li>
 *   <li><b>Rule-level</b>: {@link #DefaultRuleContext(ProjectModel, PrismGuardConfiguration, RuleConfig)} —
 *       used by {@link io.prismguard.core.engine.AnalyzerBasedRule} to inject the calling
 *       rule's parameter map. {@link #parameter} reads from the supplied {@link RuleConfig},
 *       so analyzer classes never need a hardcoded rule ID.</li>
 * </ul>
 *
 * <p>This replaces the former {@code AnalyzerContext} wrapper class, eliminating an
 * unnecessary indirection layer.
 */
public final class DefaultRuleContext implements RuleContext {

    private final ProjectModel project;
    private final PrismGuardConfiguration configuration;
    /** Non-null only in rule-level (per-analyzer) contexts. */
    private final RuleConfig ruleConfig;

    /**
     * Analysis-level context — no per-rule parameter injection.
     * {@link #parameter} returns the supplied {@code defaultValue}.
     */
    public DefaultRuleContext(ProjectModel project, PrismGuardConfiguration configuration) {
        this(project, configuration, null);
    }

    /**
     * Rule-level context — parameters are resolved from {@code ruleConfig}.
     * Created by {@link io.prismguard.core.engine.AnalyzerBasedRule} per
     * {@code analyze()} invocation.
     */
    public DefaultRuleContext(ProjectModel project, PrismGuardConfiguration configuration,
                               RuleConfig ruleConfig) {
        this.project = Objects.requireNonNull(project, "project must not be null");
        this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
        this.ruleConfig = ruleConfig; // nullable
    }

    @Override
    public ProjectModel project() {
        return project;
    }

    @Override
    public PrismGuardConfiguration configuration() {
        return configuration;
    }

    @Override
    public Path projectRoot() {
        return configuration.getProjectRoot();
    }

    /**
     * Returns the rule-scoped parameter from the injected {@link RuleConfig},
     * or {@code defaultValue} when no rule config is present or the parameter is absent.
     */
    @Override
    public <T> T parameter(String name, T defaultValue) {
        if (ruleConfig != null) return ruleConfig.getParameter(name, defaultValue);
        return defaultValue;
    }
}
