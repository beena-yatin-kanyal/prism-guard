package io.prismguard.cli;

import io.prismguard.api.config.PrismGuardConfiguration;
import io.prismguard.api.config.ReportConfiguration;
import io.prismguard.api.model.AnalysisReport;
import io.prismguard.core.config.YamlConfigurationLoader;
import io.prismguard.engine.AnalysisEngine;
import io.prismguard.reporting.ReportingService;
import org.fusesource.jansi.Ansi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Sub-command that runs the full architecture analysis pipeline on a Java/Spring Boot project.
 * <p>
 * Workflow:
 * <ol>
 *   <li>Validate project path</li>
 *   <li>Load YAML configuration, apply CLI overrides</li>
 *   <li>Run analysis (with optional live progress)</li>
 *   <li>Generate reports in the requested format(s)</li>
 *   <li>Apply exit-code policies (fail-on-critical, fail-score-below)</li>
 * </ol>
 */
@Component
@Command(
    name = "analyze",
    description = "Analyze a Java/Spring Boot project for architecture issues",
    mixinStandardHelpOptions = true
)
public class AnalyzeCommand implements Runnable {

    // ── Parameters & options ─────────────────────────────────────────────────

    @Parameters(
        index = "0",
        description = "Path to the project root directory (default: current directory)",
        defaultValue = "."
    )
    private Path projectPath;

    @Option(
        names = {"-f", "--format"},
        description = "Output format (default: html)",
        defaultValue = "html"
    )
    private String format;

    @Option(
        names = {"-o", "--output"},
        description = "Output directory for reports (default: <project>/prismguard-reports)"
    )
    private Path outputDirectory;

    @Option(
        names = {"-c", "--config"},
        description = "Path to a prismguard.yml config file (default: <project>/prismguard.yml)"
    )
    private Path configFile;

    @Option(
        names = {"--fail-on-critical"},
        description = "Exit with code 1 if any critical issues are found"
    )
    private boolean failOnCritical;

    @Option(
        names = {"--fail-score-below"},
        description = "Exit with code 1 if the overall score is below this threshold (0 = disabled)",
        defaultValue = "0"
    )
    private int failScoreBelow;

    @Option(
        names = {"--no-progress"},
        description = "Disable the live progress display during analysis"
    )
    private boolean noProgress;

    @Option(
        names = {"--exclude"},
        description = "Comma-separated glob patterns to exclude from analysis",
        split = ","
    )
    private List<String> excludePatterns;

    @Option(
        names = {"--parallel"},
        description = "Number of parallel analysis threads (0 = auto-detect from CPU cores)",
        defaultValue = "0"
    )
    private int parallelism;

    // ── Dependencies ─────────────────────────────────────────────────────────

    @Autowired
    private AnalysisEngine analysisEngine;

    @Autowired
    private ReportingService reportingService;

    @Autowired
    private YamlConfigurationLoader configLoader;

    // ── Runnable ─────────────────────────────────────────────────────────────

    @Override
    public void run() {
        // 1. Resolve and validate project path
        Path resolvedProject = projectPath.toAbsolutePath().normalize();
        if (!Files.exists(resolvedProject)) {
            System.err.println(Ansi.ansi()
                .fgRed().bold().a("Error: Project path does not exist: ").reset()
                .a(resolvedProject.toString()));
            throw new CommandLine.ParameterException(
                new CommandLine(this),
                "Project path does not exist: " + resolvedProject
            );
        }
        if (!Files.isDirectory(resolvedProject)) {
            System.err.println(Ansi.ansi()
                .fgRed().bold().a("Error: Project path is not a directory: ").reset()
                .a(resolvedProject.toString()));
            throw new CommandLine.ParameterException(
                new CommandLine(this),
                "Project path is not a directory: " + resolvedProject
            );
        }

        // 2. Build analysis configuration (YAML + CLI overrides)
        PrismGuardConfiguration config = buildConfiguration(resolvedProject);

        // 3. Build report configuration (YAML + CLI overrides)
        ReportConfiguration reportConfig = buildReportConfiguration(resolvedProject);

        // 4. Run analysis
        AnalysisReport report = runAnalysis(config);

        // 5. Generate report
        try {
            Files.createDirectories(reportConfig.outputDirectory());
            List<Path> written = reportingService.generateReports(report, reportConfig);
            written.forEach(p ->
                System.out.println(Ansi.ansi().fgCyan()
                    .a("  Report saved : ").reset().bold().a(p.toAbsolutePath()).reset()));
        } catch (IOException e) {
            System.err.println(Ansi.ansi().fgRed().bold()
                .a("Error: Failed to write report files: " + e.getMessage()).reset());
        }

        // 6. Exit-code policies
        if (failOnCritical && !report.criticalFindings().isEmpty()) {
            System.err.println(Ansi.ansi().fgRed().bold()
                .a("Failing build: " + report.criticalFindings().size() + " critical finding(s) found.")
                .reset());
            System.exit(1);
        }
        if (failScoreBelow > 0 && report.getOverallScore() != null
                && report.getOverallScore().getValue() < failScoreBelow) {
            System.err.println(Ansi.ansi().fgRed().bold()
                .a(String.format(
                    "Failing build: overall score %.1f is below required threshold %d.",
                    report.getOverallScore().getValue(), failScoreBelow))
                .reset());
            System.exit(1);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Runs the analysis engine, optionally with a live progress listener.
     */
    private AnalysisReport runAnalysis(PrismGuardConfiguration config) {
        if (noProgress) {
            return analysisEngine.analyze(config);
        }
        CliProgressListener progressListener = new CliProgressListener();
        return analysisEngine.analyzeWithProgress(config, progressListener);
    }

    /**
     * Loads configuration from YAML, then applies CLI overrides.
     * CLI options always win over file-based defaults.
     */
    private PrismGuardConfiguration buildConfiguration(Path resolvedProject) {
        // Load base config from the YAML file (or defaults if file doesn't exist)
        PrismGuardConfiguration base;
        if (configFile != null) {
            base = configLoader.loadFromFile(configFile.toAbsolutePath().normalize());
        } else {
            base = configLoader.load(resolvedProject);
        }

        // Start a new builder copying all values from the loaded config
        PrismGuardConfiguration.Builder builder = PrismGuardConfiguration.builder()
            .projectRoot(resolvedProject)
            .ruleConfigs(base.getRuleConfigs())
            .sourceDirectories(base.getSourceDirectories())
            .ruleSeverityOverrides(base.getRuleSeverityOverrides())
            .disabledRules(base.getDisabledRules())
            .enabledCategories(base.getEnabledCategories())
            .parallelism(base.getParallelism())
            .failOnCritical(base.isFailOnCritical())
            .failOnScoreBelow(base.getFailOnScoreBelow())
            .javaVersion(base.getJavaVersion());

        // Merge exclude patterns: base + any CLI additions
        List<String> mergedExcludes = new ArrayList<>(base.getExcludePatterns());
        if (excludePatterns != null && !excludePatterns.isEmpty()) {
            mergedExcludes.addAll(excludePatterns);
        }
        builder.excludePatterns(mergedExcludes);

        // CLI overrides
        if (parallelism > 0) {
            builder.parallelism(parallelism);
        }

        return builder.build();
    }

    /**
     * Builds a {@link ReportConfiguration} from YAML base values with CLI overrides applied.
     */
    private ReportConfiguration buildReportConfiguration(Path resolvedProject) {
        ReportConfiguration base;
        if (configFile != null) {
            base = configLoader.loadReportConfigFromFile(configFile.toAbsolutePath().normalize());
        } else {
            base = configLoader.loadReportConfig(resolvedProject);
        }

        String resolvedFormat = (format != null && !format.isBlank())
                ? format : base.outputFormat();

        Path resolvedDir = outputDirectory != null
                ? outputDirectory.toAbsolutePath().normalize()
                : (base.outputDirectory() != null
                        ? base.outputDirectory()
                        : resolvedProject.resolve("prismguard-reports"));

        return new ReportConfiguration(resolvedFormat, resolvedDir);
    }
}
