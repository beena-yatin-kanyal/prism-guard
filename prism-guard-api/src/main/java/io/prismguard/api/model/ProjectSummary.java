package io.prismguard.api.model;

import java.nio.file.Path;
import java.util.List;

/**
 * High-level metadata about the analysed project, derived during the scanning phase.
 */
public record ProjectSummary(
        String projectName,
        Path projectRoot,
        int totalFiles,
        int totalClasses,
        int totalMethods,
        int totalLines,
        List<String> modules,
        List<String> detectedFrameworks,
        boolean isMultiModule,
        boolean hasSpringBoot,
        boolean hasMaven,
        boolean hasGradle,
        String javaVersion
) {

    public ProjectSummary {
        modules = modules == null ? List.of() : List.copyOf(modules);
        detectedFrameworks = detectedFrameworks == null ? List.of() : List.copyOf(detectedFrameworks);
    }
}
