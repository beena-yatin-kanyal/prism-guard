package io.prismguard.api.model;

import java.util.Objects;

/**
 * Represents a quality score in the range [0.0, 100.0].
 * Immutable value type.
 */
public final class Score {

    public static final Score PERFECT = Score.of(100.0);
    public static final Score ZERO = Score.of(0.0);

    private final double value;

    private Score(double value) {
        if (value < 0.0 || value > 100.0) {
            throw new IllegalArgumentException("Score value must be in [0.0, 100.0], got: " + value);
        }
        this.value = value;
    }

    /**
     * Creates a new {@link Score} from a double value clamped to [0.0, 100.0].
     */
    public static Score of(double value) {
        double clamped = Math.max(0.0, Math.min(100.0, value));
        return new Score(clamped);
    }

    public double getValue() {
        return value;
    }

    /**
     * Returns the letter grade for this score.
     * A: 90-100, B: 75-89, C: 60-74, D: 40-59, F: &lt;40
     */
    public String grade() {
        if (value >= 90.0) return "A";
        if (value >= 75.0) return "B";
        if (value >= 60.0) return "C";
        if (value >= 40.0) return "D";
        return "F";
    }

    /**
     * Returns a human-readable label for this score.
     */
    public String label() {
        if (value >= 90.0) return "Excellent";
        if (value >= 75.0) return "Good";
        if (value >= 60.0) return "Fair";
        if (value >= 40.0) return "Poor";
        return "Critical";
    }

    /**
     * Returns {@code true} if the score meets the configured passing threshold.
     *
     * @param threshold the minimum passing score (from {@code failOnScoreBelow}).
     *                  A value of {@code 0} means no gate is configured — always passes.
     */
    public boolean isPassing(int threshold) {
        return threshold <= 0 || value >= threshold;
    }

    /**
     * Returns a new {@link Score} with {@code penalty} subtracted, clamped to 0.
     */
    public Score subtract(double penalty) {
        return Score.of(value - penalty);
    }

    /**
     * Returns whichever score is lower.
     */
    public Score min(Score other) {
        Objects.requireNonNull(other, "other must not be null");
        return this.value <= other.value ? this : other;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Score score)) return false;
        return Double.compare(score.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.format("%.1f/100 (%s)", value, grade());
    }
}
