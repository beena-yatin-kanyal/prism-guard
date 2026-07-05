package io.prismguard.test;

import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;

import java.util.List;

import org.assertj.core.api.Assertions;

/**
 * Fluent assertions for {@link Finding} lists, built on AssertJ.
 *
 * <p>Usage:
 * <pre>{@code
 * List<Finding> findings = rule.check(ctx);
 *
 * FindingAssert.assertThat(findings)
 *     .hasSize(1)
 *     .first()
 *     .hasRuleId("JAVA002")
 *     .hasSeverity(FindingSeverity.MEDIUM)
 *     .messageContains("50 lines")
 *     .filePathEndsWith("FooService.java");
 * }</pre>
 */
public class FindingAssert {

    private final List<Finding> findings;

    private FindingAssert(List<Finding> findings) {
        this.findings = findings;
    }

    public static FindingAssert assertThat(List<Finding> findings) {
        return new FindingAssert(findings);
    }

    public FindingAssert isEmpty() {
        Assertions.assertThat(findings).isEmpty();
        return this;
    }

    public FindingAssert isNotEmpty() {
        Assertions.assertThat(findings).isNotEmpty();
        return this;
    }

    public FindingAssert hasSize(int expected) {
        Assertions.assertThat(findings).hasSize(expected);
        return this;
    }

    public FindingAssert hasSizeGreaterThan(int min) {
        Assertions.assertThat(findings.size()).isGreaterThan(min);
        return this;
    }

    /** Returns a single-finding assertion for the finding at the given index. */
    public SingleFindingAssert atIndex(int index) {
        Assertions.assertThat(findings.size()).isGreaterThan(index);
        return new SingleFindingAssert(findings.get(index));
    }

    /** Returns a single-finding assertion for the first finding. */
    public SingleFindingAssert first() {
        return atIndex(0);
    }

    /** Asserts that all findings have the given rule ID. */
    public FindingAssert allHaveRuleId(String ruleId) {
        findings.forEach(f -> Assertions.assertThat(f.getRuleId()).isEqualTo(ruleId));
        return this;
    }

    /** Asserts that all findings have at least the given severity. */
    public FindingAssert allHaveSeverityAtLeast(FindingSeverity minSeverity) {
        findings.forEach(f ->
            Assertions.assertThat(f.getSeverity().isAtLeast(minSeverity))
                .as("Expected severity >= %s but was %s for finding: %s",
                    minSeverity, f.getSeverity(), f.getMessage())
                .isTrue()
        );
        return this;
    }

    // ── Inner class: SingleFindingAssert ──────────────────────────────────────

    public static class SingleFindingAssert {

        private final Finding finding;

        SingleFindingAssert(Finding finding) {
            this.finding = finding;
        }

        public SingleFindingAssert hasRuleId(String expected) {
            Assertions.assertThat(finding.getRuleId()).isEqualTo(expected);
            return this;
        }

        public SingleFindingAssert hasSeverity(FindingSeverity expected) {
            Assertions.assertThat(finding.getSeverity()).isEqualTo(expected);
            return this;
        }

        public SingleFindingAssert messageContains(String substring) {
            Assertions.assertThat(finding.getMessage()).contains(substring);
            return this;
        }

        public SingleFindingAssert messageMatches(String regex) {
            Assertions.assertThat(finding.getMessage()).matches(regex);
            return this;
        }

        public SingleFindingAssert filePathEndsWith(String suffix) {
            Assertions.assertThat(finding.getFilePath()).endsWith(suffix);
            return this;
        }

        public SingleFindingAssert atLine(int expectedLine) {
            Assertions.assertThat(finding.getLineNumber()).isEqualTo(expectedLine);
            return this;
        }

        public SingleFindingAssert atLineGreaterThan(int minLine) {
            Assertions.assertThat(finding.getLineNumber()).isGreaterThan(minLine);
            return this;
        }

        public SingleFindingAssert hasRecommendation() {
            Assertions.assertThat(finding.getRecommendation()).isNotBlank();
            return this;
        }

        /** Returns the underlying {@link Finding} for custom assertions. */
        public Finding get() {
            return finding;
        }
    }
}
