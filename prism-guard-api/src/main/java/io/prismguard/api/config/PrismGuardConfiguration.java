package io.prismguard.api.config;

import io.prismguard.api.model.FindingSeverity;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable configuration object for a Prism Guard analysis run.
 * Use {@link Builder} to create instances; loaded by {@code YamlConfigurationLoader}.
 */
public final class PrismGuardConfiguration {

    private final Path projectRoot;
    private final List<String> sourceDirectories;
    private final List<String> excludePatterns;
    private final Map<String, FindingSeverity> ruleSeverityOverrides;
    private final Set<String> disabledRules;
    private final Set<String> enabledCategories;
    private final int parallelism;
    private final boolean failOnCritical;
    private final int failOnScoreBelow;
    private final Map<String, RuleConfig> ruleConfigs;
    private final ScoringConfig scoringConfig;
    private final String javaVersion;

    private PrismGuardConfiguration(Builder builder) {
        this.projectRoot = Objects.requireNonNull(builder.projectRoot, "projectRoot must not be null");
        this.sourceDirectories = List.copyOf(builder.sourceDirectories);
        this.excludePatterns = List.copyOf(builder.excludePatterns);
        this.ruleSeverityOverrides = Collections.unmodifiableMap(new HashMap<>(builder.ruleSeverityOverrides));
        this.disabledRules = Collections.unmodifiableSet(new HashSet<>(builder.disabledRules));
        this.enabledCategories = Collections.unmodifiableSet(new HashSet<>(builder.enabledCategories));
        this.parallelism = builder.parallelism;
        this.failOnCritical = builder.failOnCritical;
        this.failOnScoreBelow = builder.failOnScoreBelow;
        this.ruleConfigs = Collections.unmodifiableMap(new HashMap<>(builder.ruleConfigs));
        this.scoringConfig = builder.scoringConfig != null ? builder.scoringConfig : ScoringConfig.defaults();
        this.javaVersion = builder.javaVersion != null ? builder.javaVersion : "21";
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public List<String> getSourceDirectories() {
        return sourceDirectories;
    }

    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    public Map<String, FindingSeverity> getRuleSeverityOverrides() {
        return ruleSeverityOverrides;
    }

    public Set<String> getDisabledRules() {
        return disabledRules;
    }

    public Set<String> getEnabledCategories() {
        return enabledCategories;
    }

    public int getParallelism() {
        return parallelism;
    }

    public boolean isFailOnCritical() {
        return failOnCritical;
    }

    public int getFailOnScoreBelow() {
        return failOnScoreBelow;
    }

    public Map<String, RuleConfig> getRuleConfigs() {
        return ruleConfigs;
    }

    public ScoringConfig getScoringConfig() {
        return scoringConfig;
    }

    /**
     * Returns the Java language version used to configure the source parser (e.g. {@code "17"}, {@code "21"}).
     * Defaults to {@code "21"} when not specified in {@code prismguard.yml}.
     */
    public String getJavaVersion() {
        return javaVersion;
    }

    /**
     * Returns the {@link RuleConfig} for the given rule ID, or an empty config if not found.
     */
    public RuleConfig getRuleConfig(String ruleId) {
        return ruleConfigs.getOrDefault(ruleId, RuleConfig.empty(ruleId));
    }

    /**
     * Returns {@code true} if the given rule ID is not in the disabled-rules set.
     */
    public boolean isRuleEnabled(String ruleId) {
        if (ruleId == null) return false;
        if (disabledRules.contains(ruleId)) return false;
        RuleConfig rc = ruleConfigs.get(ruleId);
        if (rc != null && rc.getEnabled() != null) return rc.getEnabled();
        return true;
    }

    /**
     * Returns {@code true} if the given category (by name or short code) is enabled.
     * When {@code enabledCategories} is empty, all categories are considered enabled.
     */
    public boolean isCategoryEnabled(String category) {
        if (category == null) return false;
        if (enabledCategories.isEmpty()) return true;
        return enabledCategories.contains(category);
    }

    public static Builder builder() {
        return new Builder();
    }

    // -------------------------------------------------------------------------
    // ScoringConfig — per-project penalty model for ScoreCalculator
    // -------------------------------------------------------------------------

    /**
     * Configures the penalty model used by {@code ScoreCalculator}.
     *
     * <p>Loaded from the {@code scoring:} section in {@code prismguard.yml}.
     * When absent, {@link #defaults()} applies (which matches the prior hardcoded values).
     *
     * <p>Example YAML:
     * <pre>
     * scoring:
     *   critical:
     *     penalty: 15
     *     cap: 45
     *   high:
     *     penalty: 8
     *     cap: 32
     *   medium:
     *     penalty: 3
     *     cap: 15
     *   low:
     *     penalty: 1
     *     cap: 5
     * </pre>
     */
    public static final class ScoringConfig {

        private final double criticalPenalty;
        private final double criticalCap;
        private final double highPenalty;
        private final double highCap;
        private final double mediumPenalty;
        private final double mediumCap;
        private final double lowPenalty;
        private final double lowCap;

        public ScoringConfig(double criticalPenalty, double criticalCap,
                              double highPenalty, double highCap,
                              double mediumPenalty, double mediumCap,
                              double lowPenalty, double lowCap) {
            this.criticalPenalty = criticalPenalty;
            this.criticalCap     = criticalCap;
            this.highPenalty     = highPenalty;
            this.highCap         = highCap;
            this.mediumPenalty   = mediumPenalty;
            this.mediumCap       = mediumCap;
            this.lowPenalty      = lowPenalty;
            this.lowCap          = lowCap;
        }

        /** Returns the default penalty model matching prior hardcoded values. */
        public static ScoringConfig defaults() {
            return new ScoringConfig(15, 45, 8, 32, 3, 15, 1, 5);
        }

        public double getCriticalPenalty() { return criticalPenalty; }
        public double getCriticalCap()     { return criticalCap; }
        public double getHighPenalty()     { return highPenalty; }
        public double getHighCap()         { return highCap; }
        public double getMediumPenalty()   { return mediumPenalty; }
        public double getMediumCap()       { return mediumCap; }
        public double getLowPenalty()      { return lowPenalty; }
        public double getLowCap()          { return lowCap; }
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static final class Builder {

        private Path projectRoot;
        private List<String> sourceDirectories = List.of("src/main/java");
        private List<String> excludePatterns = List.of();
        private Map<String, FindingSeverity> ruleSeverityOverrides = new HashMap<>();
        private Set<String> disabledRules = new HashSet<>();
        private Set<String> enabledCategories = new HashSet<>();
        private int parallelism = Runtime.getRuntime().availableProcessors();
        private boolean failOnCritical = false;
        private int failOnScoreBelow = 0;
        private Map<String, RuleConfig> ruleConfigs = new HashMap<>();
        private ScoringConfig scoringConfig;
        private String javaVersion;

        private Builder() {
        }

        public Builder projectRoot(Path projectRoot) {
            this.projectRoot = projectRoot;
            return this;
        }

        public Builder sourceDirectories(List<String> sourceDirectories) {
            this.sourceDirectories = sourceDirectories != null ? sourceDirectories : List.of("src/main/java");
            return this;
        }

        public Builder excludePatterns(List<String> excludePatterns) {
            this.excludePatterns = excludePatterns != null ? excludePatterns : List.of();
            return this;
        }

        public Builder ruleSeverityOverrides(Map<String, FindingSeverity> overrides) {
            if (overrides != null) this.ruleSeverityOverrides.putAll(overrides);
            return this;
        }

        public Builder disabledRules(Set<String> disabledRules) {
            if (disabledRules != null) this.disabledRules.addAll(disabledRules);
            return this;
        }

        public Builder disableRule(String ruleId) {
            this.disabledRules.add(ruleId);
            return this;
        }

        public Builder enabledCategories(Set<String> enabledCategories) {
            if (enabledCategories != null) this.enabledCategories.addAll(enabledCategories);
            return this;
        }

        public Builder parallelism(int parallelism) {
            this.parallelism = Math.max(1, parallelism);
            return this;
        }

        public Builder failOnCritical(boolean failOnCritical) {
            this.failOnCritical = failOnCritical;
            return this;
        }

        public Builder failOnScoreBelow(int threshold) {
            this.failOnScoreBelow = threshold;
            return this;
        }

        public Builder ruleConfigs(Map<String, RuleConfig> ruleConfigs) {
            if (ruleConfigs != null) this.ruleConfigs.putAll(ruleConfigs);
            return this;
        }

        public Builder ruleConfig(String id, RuleConfig config) {
            this.ruleConfigs.put(id, config);
            return this;
        }

        public Builder scoringConfig(ScoringConfig scoringConfig) {
            this.scoringConfig = scoringConfig;
            return this;
        }

        public Builder javaVersion(String javaVersion) {
            this.javaVersion = javaVersion;
            return this;
        }

        public PrismGuardConfiguration build() {
            return new PrismGuardConfiguration(this);
        }
    }
}
