package io.prismguard.api.model;

/**
 * Severity levels for rule findings, ordered from most to least severe.
 */
public enum FindingSeverity {

    CRITICAL(1, "Critical"),
    HIGH(2, "High"),
    MEDIUM(3, "Medium"),
    LOW(4, "Low"),
    INFO(5, "Info");

    private final int order;
    private final String displayName;

    FindingSeverity(int order, String displayName) {
        this.order = order;
        this.displayName = displayName;
    }

    public int getOrder() {
        return order;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns {@code true} if this severity is at least as severe as {@code other}.
     * E.g., {@code CRITICAL.isAtLeast(HIGH)} returns {@code true}.
     */
    public boolean isAtLeast(FindingSeverity other) {
        return this.order <= other.order;
    }
}
