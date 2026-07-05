package io.prismguard.api.config;

import io.prismguard.api.model.FindingSeverity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable configuration for a single rule, loaded from {@code prismguard.yml}.
 *
 * <p>All fields are optional — {@code null} means "use the framework default".
 * The engine merges: YAML user config > bundled defaults > {@code @PrismRule} annotation.
 */
public final class RuleConfig {

    private final String id;
    private final String analyzer;
    private final String title;
    private final String description;
    private final String rationale;
    private final FindingSeverity severity;
    private final String category;
    private final List<String> tags;
    private final Boolean enabled;
    private final String recommendation;
    private final Map<String, Object> parameters;

    private RuleConfig(Builder builder) {
        this.id = builder.id;
        this.analyzer = builder.analyzer;
        this.title = builder.title;
        this.description = builder.description;
        this.rationale = builder.rationale;
        this.severity = builder.severity;
        this.category = builder.category;
        this.tags = builder.tags != null ? List.copyOf(builder.tags) : List.of();
        this.enabled = builder.enabled;
        this.recommendation = builder.recommendation;
        this.parameters = builder.parameters != null
                ? Collections.unmodifiableMap(new HashMap<>(builder.parameters))
                : Map.of();
    }

    /** The rule ID, e.g. {@code "JAVA002"}. */
    public String getId() { return id; }

    /**
     * The analyzer type that implements this rule, e.g. {@code "long-method"}.
     * Corresponds to {@link io.prismguard.api.annotation.Analyzer#type()} on the
     * analyzer class. A {@code null} or blank value means the rule has no associated
     * analyzer and will be excluded from execution.
     */
    public String getAnalyzer() { return analyzer; }

    /** Display name; {@code null} means use annotation default. */
    public String getTitle() { return title; }

    /** Short description; {@code null} means use annotation default. */
    public String getDescription() { return description; }

    /** Rationale; {@code null} means use annotation default. */
    public String getRationale() { return rationale; }

    /**
     * Effective severity override; {@code null} means use annotation default.
     * Note: explicit {@code ruleSeverityOverrides} in the YAML take precedence over this.
     */
    public FindingSeverity getSeverity() { return severity; }

    /** Category name; {@code null} means use annotation default. */
    public String getCategory() { return category; }

    /** Tags list; empty list means use annotation default. */
    public List<String> getTags() { return tags; }

    /**
     * Whether this rule is enabled; {@code null} means use annotation {@code enabledByDefault}.
     * Explicit {@code rules.disabled} entries still take precedence.
     */
    public Boolean getEnabled() { return enabled; }

    /**
     * Default recommendation text shown in findings; {@code null} means each rule's own text.
     */
    public String getRecommendation() { return recommendation; }

    /**
     * Default parameter values for this rule (e.g. {@code maxLines: 50}).
     */
    public Map<String, Object> getParameters() { return parameters; }

    /**
     * Retrieves a typed parameter value, falling back to {@code defaultValue}.
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String name, T defaultValue) {
        Object value = parameters.get(name);
        if (value == null) return defaultValue;
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    public static Builder builder() { return new Builder(); }

    /** Returns an empty {@code RuleConfig} for the given ID (all fields null). */
    public static RuleConfig empty(String id) {
        return builder().id(id).build();
    }

    public static final class Builder {
        private String id;
        private String analyzer;
        private String title;
        private String description;
        private String rationale;
        private FindingSeverity severity;
        private String category;
        private List<String> tags;
        private Boolean enabled;
        private String recommendation;
        private Map<String, Object> parameters;

        private Builder() {}

        public Builder id(String id)                          { this.id = id; return this; }
        public Builder analyzer(String analyzer)              { this.analyzer = analyzer; return this; }
        public Builder title(String title)                    { this.title = title; return this; }
        public Builder description(String description)        { this.description = description; return this; }
        public Builder rationale(String rationale)            { this.rationale = rationale; return this; }
        public Builder severity(FindingSeverity severity)     { this.severity = severity; return this; }
        public Builder category(String category)              { this.category = category; return this; }
        public Builder tags(List<String> tags)                { this.tags = tags; return this; }
        public Builder enabled(Boolean enabled)               { this.enabled = enabled; return this; }
        public Builder recommendation(String recommendation)  { this.recommendation = recommendation; return this; }
        public Builder parameters(Map<String, Object> params) { this.parameters = params; return this; }

        public RuleConfig build() { return new RuleConfig(this); }
    }
}
