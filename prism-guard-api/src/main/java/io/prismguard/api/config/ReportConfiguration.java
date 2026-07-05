package io.prismguard.api.config;

import java.nio.file.Path;

/**
 * Lightweight configuration for report generation.
 * Separate from {@link PrismGuardConfiguration} so that analysis and output concerns
 * do not share a single configuration object.
 *
 * @param outputFormat    report format identifier (e.g. {@code "html"})
 * @param outputDirectory directory where report files are written
 */
public record ReportConfiguration(String outputFormat, Path outputDirectory) {
}
