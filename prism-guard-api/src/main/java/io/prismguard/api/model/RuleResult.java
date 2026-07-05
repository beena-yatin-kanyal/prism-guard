package io.prismguard.api.model;

import io.prismguard.api.config.RuleConfig;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The outcome of executing a single rule against a project.
 *
 * <p>{@link RuleConfig} is the single authoritative source of rule metadata —
 * there is no separate descriptor copy to keep in sync.
 */
public record RuleResult(
        RuleConfig ruleConfig,
        List<Finding> findings,
        Duration executionTime,
        boolean succeeded,
        String errorMessage
) {

    public RuleResult {
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    public boolean hasFindings() {
        return !findings.isEmpty();
    }

    public int findingCount() {
        return findings.size();
    }

    public Map<FindingSeverity, Long> findingCountBySeverity() {
        return findings.stream()
                .collect(Collectors.groupingBy(Finding::getSeverity, Collectors.counting()));
    }

    /**
     * Creates a successful result.
     */
    public static RuleResult success(RuleConfig ruleConfig, List<Finding> findings, Duration duration) {
        return new RuleResult(ruleConfig, findings, duration, true, null);
    }

    /**
     * Creates a failure result when a rule throws an unexpected exception.
     */
    public static RuleResult failure(RuleConfig ruleConfig, Duration duration, String error) {
        return new RuleResult(ruleConfig, List.of(), duration, false, error);
    }
}
