package io.prismguard.api.rule;

import io.prismguard.api.config.RuleConfig;
import io.prismguard.api.model.ProjectModel;
import io.prismguard.api.model.RuleResult;

/**
 * Contract for all analysis rules.
 *
 * <p>Implementations must be thread-safe since they may be executed concurrently.
 * The {@link #analyze(RuleContext)} method must not throw; any unexpected error
 * should be caught internally and reported as a {@link RuleResult#failure} result.
 */
public interface Rule {

    /**
     * Returns the {@link RuleConfig} describing this rule — the single authoritative
     * source of rule metadata.
     */
    RuleConfig ruleConfig();

    /**
     * Analyses the project and returns findings.
     *
     * @param context runtime context providing access to the parsed project model and configuration
     * @return a result containing zero or more findings, or a failure result on unexpected error
     */
    RuleResult analyze(RuleContext context);

    /**
     * Returns {@code true} if this rule should run against the given project.
     * Implementations may override this to skip rules that are irrelevant
     * (e.g. a Spring-specific rule when the project has no Spring dependency).
     */
    default boolean isApplicable(ProjectModel project) {
        return true;
    }
}
