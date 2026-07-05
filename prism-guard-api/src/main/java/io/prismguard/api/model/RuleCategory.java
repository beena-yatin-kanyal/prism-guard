package io.prismguard.api.model;

/**
 * Categorisation of analysis rules.
 */
public enum RuleCategory {

    ARCHITECTURE("Architecture", "arch"),
    SPRING_BOOT("Spring Boot", "spring"),
    JAVA_QUALITY("Java Quality", "java"),
    SECURITY("Security", "security"),
    PERFORMANCE("Performance", "perf"),
    MAINTAINABILITY("Maintainability", "maint"),
    SOLID("SOLID Principles", "solid"),
    CLEAN_ARCHITECTURE("Clean Architecture", "clean-arch"),
    JPA_HIBERNATE("JPA/Hibernate", "jpa"),
    TRANSACTION("Transaction Management", "tx"),
    REST_API("REST API Design", "rest"),
    MICROSERVICES("Microservices", "micro"),
    CONFIGURATION("Configuration", "config"),
    OBSERVABILITY("Observability", "obs"),
    DEPENDENCY_INJECTION("Dependency Injection", "di");

    private final String displayName;
    private final String shortCode;

    RuleCategory(String displayName, String shortCode) {
        this.displayName = displayName;
        this.shortCode = shortCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getShortCode() {
        return shortCode;
    }
}
