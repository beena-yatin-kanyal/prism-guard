package io.prismguard.reporting;

import io.prismguard.api.config.ReportConfiguration;
import io.prismguard.api.model.AnalysisReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orchestrates report generation across all registered {@link ReportGenerator} implementations.
 */
@Component
public class ReportingService {

    private static final Logger log = LoggerFactory.getLogger(ReportingService.class);

    private final Map<String, ReportGenerator> generatorsByFormat;

    public ReportingService(List<ReportGenerator> generators) {
        this.generatorsByFormat = generators.stream()
                .collect(Collectors.toMap(ReportGenerator::format, Function.identity()));
    }

    /**
     * Generates the report for the configured output format.
     *
     * @param report       the completed analysis report
     * @param reportConfig output format and directory
     * @return list of output file paths written
     * @throws IOException if writing fails
     */
    public List<Path> generateReports(AnalysisReport report, ReportConfiguration reportConfig) throws IOException {
        String outputFormat = reportConfig.outputFormat() != null
                ? reportConfig.outputFormat().toLowerCase().trim()
                : "html";

        Path outputDirectory = reportConfig.outputDirectory();
        if (outputDirectory != null) {
            Files.createDirectories(outputDirectory);
        }

        List<Path> written = new ArrayList<>();

        ReportGenerator selected = generatorsByFormat.get(outputFormat);
        if (selected == null) {
            log.warn("No ReportGenerator found for format '{}'. Available formats: {}",
                    outputFormat, generatorsByFormat.keySet());
            return written;
        }

        log.debug("Running report generator: {}", selected.format());
        Path reportFile;
        try {
            reportFile = selected.generate(report, outputDirectory);
        } catch (IOException e) {
            log.error("Report generator '{}' failed: {}", selected.format(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Report generator '{}' failed unexpectedly: {}", selected.format(), e.getMessage(), e);
            throw new IOException("Report generator '" + selected.format() + "' failed: " + e.getMessage(), e);
        }

        if (reportFile != null) {
            written.add(reportFile);
        }

        return written;
    }
}
