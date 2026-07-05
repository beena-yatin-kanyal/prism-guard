package io.prismguard.api.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The complete outcome of a Prism Guard analysis run.
 * Immutable; use {@link Builder} to create instances.
 */
public final class AnalysisReport {

    private final Score overallScore;
    private final Map<String, Score> moduleScores;
    private final Map<RuleCategory, Score> categoryScores;
    private final List<Finding> allFindings;
    private final List<RuleResult> ruleResults;
    private final ProjectSummary projectSummary;
    private final Duration totalDuration;
    private final Instant analyzedAt;
    private final String prismGuardVersion;
    private final int failOnScoreBelow;

    private AnalysisReport(Builder builder) {
        this.overallScore = Objects.requireNonNull(builder.overallScore, "overallScore must not be null");
        this.moduleScores = Map.copyOf(builder.moduleScores);
        this.categoryScores = Map.copyOf(builder.categoryScores);
        this.allFindings = List.copyOf(builder.allFindings);
        this.ruleResults = List.copyOf(builder.ruleResults);
        this.projectSummary = Objects.requireNonNull(builder.projectSummary, "projectSummary must not be null");
        this.totalDuration = Objects.requireNonNull(builder.totalDuration, "totalDuration must not be null");
        this.analyzedAt = builder.analyzedAt != null ? builder.analyzedAt : Instant.now();
        this.prismGuardVersion = builder.prismGuardVersion != null ? builder.prismGuardVersion : "1.0.0";
        this.failOnScoreBelow = builder.failOnScoreBelow;
    }

    public Score getOverallScore() {
        return overallScore;
    }

    public Map<String, Score> getModuleScores() {
        return moduleScores;
    }

    public Map<RuleCategory, Score> getCategoryScores() {
        return categoryScores;
    }

    public List<Finding> getAllFindings() {
        return allFindings;
    }

    public List<RuleResult> getRuleResults() {
        return ruleResults;
    }

    public ProjectSummary getProjectSummary() {
        return projectSummary;
    }

    public Duration getTotalDuration() {
        return totalDuration;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public String getPrismGuardVersion() {
        return prismGuardVersion;
    }

    /**
     * The configured minimum passing score ({@code failOnScoreBelow}).
     * A value of {@code 0} means no gate is configured.
     */
    public int getFailOnScoreBelow() {
        return failOnScoreBelow;
    }

    /**
     * Groups all findings by their severity.
     */
    public Map<FindingSeverity, List<Finding>> findingsBySeverity() {
        return allFindings.stream()
                .collect(Collectors.groupingBy(Finding::getSeverity));
    }

    /**
     * Groups all findings by their rule category.
     */
    public Map<RuleCategory, List<Finding>> findingsByCategory() {
        return allFindings.stream()
                .collect(Collectors.groupingBy(Finding::getCategory));
    }

    /**
     * Returns only CRITICAL severity findings.
     */
    public List<Finding> criticalFindings() {
        return allFindings.stream()
                .filter(f -> f.getSeverity() == FindingSeverity.CRITICAL)
                .toList();
    }

    /**
     * Returns the top {@code n} findings ordered by severity (most severe first).
     */
    public List<Finding> topIssues(int n) {
        return allFindings.stream()
                .sorted(Comparator.comparingInt(f -> f.getSeverity().getOrder()))
                .limit(n)
                .toList();
    }

    public static Builder builder() {
        return new Builder();
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static final class Builder {

        private Score overallScore;
        private Map<String, Score> moduleScores = Map.of();
        private Map<RuleCategory, Score> categoryScores = new EnumMap<>(RuleCategory.class);
        private List<Finding> allFindings = List.of();
        private List<RuleResult> ruleResults = List.of();
        private ProjectSummary projectSummary;
        private Duration totalDuration;
        private Instant analyzedAt;
        private String prismGuardVersion;
        private int failOnScoreBelow;

        private Builder() {
        }

        public Builder overallScore(Score overallScore) {
            this.overallScore = overallScore;
            return this;
        }

        public Builder moduleScores(Map<String, Score> moduleScores) {
            this.moduleScores = moduleScores != null ? moduleScores : Map.of();
            return this;
        }

        public Builder categoryScores(Map<RuleCategory, Score> categoryScores) {
            this.categoryScores = categoryScores != null ? categoryScores : new EnumMap<>(RuleCategory.class);
            return this;
        }

        public Builder allFindings(List<Finding> allFindings) {
            this.allFindings = allFindings != null ? allFindings : List.of();
            return this;
        }

        public Builder ruleResults(List<RuleResult> ruleResults) {
            this.ruleResults = ruleResults != null ? ruleResults : List.of();
            return this;
        }

        public Builder projectSummary(ProjectSummary projectSummary) {
            this.projectSummary = projectSummary;
            return this;
        }

        public Builder totalDuration(Duration totalDuration) {
            this.totalDuration = totalDuration;
            return this;
        }

        public Builder analyzedAt(Instant analyzedAt) {
            this.analyzedAt = analyzedAt;
            return this;
        }

        public Builder prismGuardVersion(String prismGuardVersion) {
            this.prismGuardVersion = prismGuardVersion;
            return this;
        }

        public Builder failOnScoreBelow(int threshold) {
            this.failOnScoreBelow = threshold;
            return this;
        }

        public AnalysisReport build() {
            return new AnalysisReport(this);
        }
    }
}
