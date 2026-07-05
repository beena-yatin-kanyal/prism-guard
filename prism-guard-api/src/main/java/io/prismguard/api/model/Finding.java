package io.prismguard.api.model;

import java.util.Objects;

/**
 * A single violation or observation produced by a rule analysis.
 * Immutable after construction; use {@link Builder} to create instances.
 */
public final class Finding {

    private final String ruleId;
    private final String ruleName;
    private final FindingSeverity severity;
    private final RuleCategory category;
    private final String message;
    private final String recommendation;
    private final String filePath;
    private final int lineNumber;
    private final int columnNumber;
    private final String codeSnippet;

    private Finding(Builder builder) {
        this.ruleId = Objects.requireNonNull(builder.ruleId, "ruleId must not be null");
        this.ruleName = Objects.requireNonNull(builder.ruleName, "ruleName must not be null");
        this.severity = Objects.requireNonNull(builder.severity, "severity must not be null");
        this.category = Objects.requireNonNull(builder.category, "category must not be null");
        this.message = Objects.requireNonNull(builder.message, "message must not be null");
        this.recommendation = builder.recommendation;
        this.filePath = builder.filePath;
        this.lineNumber = builder.lineNumber;
        this.columnNumber = builder.columnNumber;
        this.codeSnippet = builder.codeSnippet;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public FindingSeverity getSeverity() {
        return severity;
    }

    public RuleCategory getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Finding finding)) return false;
        return lineNumber == finding.lineNumber
                && columnNumber == finding.columnNumber
                && Objects.equals(ruleId, finding.ruleId)
                && Objects.equals(filePath, finding.filePath)
                && Objects.equals(message, finding.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleId, filePath, lineNumber, columnNumber, message);
    }

    @Override
    public String toString() {
        return "Finding{ruleId='" + ruleId + "', severity=" + severity
                + ", file='" + filePath + ":" + lineNumber + "', message='" + message + "'}";
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static final class Builder {

        private String ruleId;
        private String ruleName;
        private FindingSeverity severity;
        private RuleCategory category;
        private String message;
        private String recommendation;
        private String filePath;
        private int lineNumber;
        private int columnNumber;
        private String codeSnippet;

        private Builder() {
        }

        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public Builder ruleName(String ruleName) {
            this.ruleName = ruleName;
            return this;
        }

        public Builder severity(FindingSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder category(RuleCategory category) {
            this.category = category;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder recommendation(String recommendation) {
            this.recommendation = recommendation;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder lineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public Builder columnNumber(int columnNumber) {
            this.columnNumber = columnNumber;
            return this;
        }

        public Builder codeSnippet(String codeSnippet) {
            this.codeSnippet = codeSnippet;
            return this;
        }

        public Finding build() {
            return new Finding(this);
        }
    }
}
