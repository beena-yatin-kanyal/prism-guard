package io.prismguard.reporting;

import io.prismguard.api.model.AnalysisReport;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Strategy interface for generating analysis reports in different formats.
 */
public interface ReportGenerator {

    /**
     * Returns the format identifier for this generator.
     * Typical value: "html"
     */
    String format();

    /**
     * Generates the report, writes it to the given output directory, and returns the
     * path of the file that was written.
     *
     * @param report          the completed analysis report
     * @param outputDirectory directory where file-based reports are written
     * @return path of the written report file
     * @throws IOException if writing fails
     */
    Path generate(AnalysisReport report, Path outputDirectory) throws IOException;
}
