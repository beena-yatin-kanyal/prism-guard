package io.prismguard.api.rule;

import io.prismguard.api.config.PrismGuardConfiguration;
import io.prismguard.api.model.ProjectModel;

import java.nio.file.Path;

/**
 * Provides rules with everything they need to perform analysis.
 */
public interface RuleContext {

    /**
     * Returns the fully parsed project model.
     */
    ProjectModel project();

    /**
     * Returns the active configuration.
     */
    PrismGuardConfiguration configuration();

    /**
     * Returns the root directory of the project under analysis.
     */
    Path projectRoot();

    /**
     * Convenience method — delegates to {@link PrismGuardConfiguration#isRuleEnabled(String)}.
     */
    default boolean isRuleEnabled(String ruleId) {
        return configuration().isRuleEnabled(ruleId);
    }

    /**
     * Returns a rule-scoped parameter value, or {@code defaultValue} if absent.
     *
     * <p>Analyzer classes ({@link io.prismguard.api.annotation.Analyzer}) must use this
     * method. The engine injects the calling rule's parameters into the context before
     * invoking the analyzer, so this method always returns the correct value for the
     * current execution.
     *
     * <p>The base implementation returns {@code defaultValue}. {@link io.prismguard.core.context.DefaultRuleContext}
     * overrides it to read from the calling rule's parameter map in {@code prismguard.yml}.
     *
     * @param name         parameter name, e.g. {@code "maxLines"}
     * @param defaultValue fallback when the parameter is absent or of the wrong type
     * @param <T>          the expected type
     */
    default <T> T parameter(String name, T defaultValue) {
        return defaultValue;
    }
}
