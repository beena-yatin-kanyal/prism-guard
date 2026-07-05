package io.prismguard.core.engine;

import io.prismguard.api.config.PrismGuardConfiguration;
import io.prismguard.api.config.RuleConfig;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.ProjectModel;
import io.prismguard.api.model.RuleResult;
import io.prismguard.api.rule.Rule;
import io.prismguard.api.rule.RuleContext;
import io.prismguard.core.context.DefaultRuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Bridges a generic {@link io.prismguard.api.annotation.Analyzer}-annotated class into
 * the {@link Rule} interface for a specific rule ID defined in {@code prismguard.yml}.
 *
 * <h3>N:M mapping model</h3>
 * <p>One analyzer class can power any number of YAML-defined rules, each with
 * independent parameters. The same {@code @Analyzer(type = "long-method")} class
 * can back both {@code JAVA002} and {@code JAVA002-STRICT} with different thresholds.
 *
 * <h3>Canonical metadata</h3>
 * <p>{@link RuleConfig} is stored at construction time and exposed via {@link #ruleConfig()}.
 * It is the single authoritative source of rule metadata — no separate descriptor copy exists.
 *
 * <h3>Parameter injection</h3>
 * <p>Before invoking the {@link io.prismguard.api.annotation.Check} method, a {@link DefaultRuleContext} is created
 * with the rule's per-rule {@link RuleConfig} so analyzers read thresholds via
 * {@code context.parameter("maxLines", 50)} — no hardcoded rule ID required.
 */
public class AnalyzerBasedRule implements Rule {

    private static final Logger log = LoggerFactory.getLogger(AnalyzerBasedRule.class);

    private final Object analyzerInstance;
    private final Method checkMethod;
    private final String ruleId;

    /** Canonical rule config resolved from {@code prismguard.yml} at construction time. */
    private final RuleConfig ruleConfig;

    /**
     * @param analyzerInstance validated wrapper built by {@link AnalyzerRegistry} at startup;
     *                         the {@code @Check} method has already been located and made accessible
     * @param ruleId           the rule ID as declared in {@code prismguard.yml}
     * @param config           the active configuration; used to resolve the rule config eagerly
     */
    public AnalyzerBasedRule(AnalyzerInstance analyzerInstance, String ruleId, PrismGuardConfiguration config) {
        this.analyzerInstance = analyzerInstance.instance();
        this.checkMethod = analyzerInstance.checkMethod();
        this.ruleId = ruleId;
        this.ruleConfig = config.getRuleConfig(ruleId);
    }

    @Override
    public RuleConfig ruleConfig() {
        return ruleConfig;
    }

    @Override
    public RuleResult analyze(RuleContext context) {
        Instant start = Instant.now();
        RuleConfig effectiveConfig = context.configuration().getRuleConfig(ruleId);
        DefaultRuleContext ruleContext = new DefaultRuleContext(context.project(), context.configuration(), effectiveConfig);

        try {
            Object result = checkMethod.invoke(analyzerInstance, ruleContext);
            List<Finding> findings = AnalyzerSupport.coerceToFindings(
                    result, analyzerInstance.getClass().getName());
            List<Finding> resolved = AnalyzerSupport.resolveFromConfig(
                    ruleId, findings, effectiveConfig, context.configuration());
            return RuleResult.success(effectiveConfig, resolved, Duration.between(start, Instant.now()));
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.warn("Analyzer '{}' (rule '{}') threw: {}",
                    analyzerInstance.getClass().getSimpleName(), ruleId, cause.getMessage(), cause);
            return RuleResult.failure(effectiveConfig, Duration.between(start, Instant.now()), cause.getMessage());
        }
    }

    @Override
    public boolean isApplicable(ProjectModel project) {
        return true;
    }
}
