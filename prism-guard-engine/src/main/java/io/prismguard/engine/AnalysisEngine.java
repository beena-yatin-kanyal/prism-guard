package io.prismguard.engine;

import io.prismguard.api.config.PrismGuardConfiguration;
import io.prismguard.api.model.AnalysisReport;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.ProjectModel;
import io.prismguard.api.model.ProjectSummary;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.model.RuleResult;
import io.prismguard.api.model.Score;
import io.prismguard.api.plugin.PrismGuardPlugin;
import io.prismguard.api.rule.RuleContext;
import io.prismguard.core.context.DefaultRuleContext;
import io.prismguard.core.engine.RuleExecutor;
import io.prismguard.core.engine.ScoreCalculator;
import io.prismguard.core.plugin.ServiceLoaderPluginLoader;
import io.prismguard.core.scanner.JavaProjectScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Top-level entry point for executing a Prism Guard analysis.
 *
 * <p>The engine orchestrates the full analysis pipeline:
 * <ol>
 *   <li>Scan the project file-system → {@link ProjectModel}</li>
 *   <li>Build a rule execution context</li>
 *   <li>Execute all registered rules → {@link List}&lt;{@link RuleResult}&gt;</li>
 *   <li>Aggregate all {@link Finding}s</li>
 *   <li>Calculate overall, category-level, and module-level scores</li>
 *   <li>Assemble and return an immutable {@link AnalysisReport}</li>
 * </ol>
 */
@Component
public class AnalysisEngine {

    private static final Logger log = LoggerFactory.getLogger(AnalysisEngine.class);
    private static final String VERSION = readVersion();

    private static String readVersion() {
        try (InputStream in = AnalysisEngine.class.getResourceAsStream("version.properties")) {
            if (in == null) return "unknown";
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("version", "unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }

    private final JavaProjectScanner scanner;
    private final RuleExecutor ruleExecutor;
    private final ScoreCalculator scoreCalculator;
    private final ServiceLoaderPluginLoader pluginLoader;

    public AnalysisEngine(JavaProjectScanner scanner,
                          RuleExecutor ruleExecutor,
                          ScoreCalculator scoreCalculator,
                          ServiceLoaderPluginLoader pluginLoader) {
        this.scanner = Objects.requireNonNull(scanner, "scanner must not be null");
        this.ruleExecutor = Objects.requireNonNull(ruleExecutor, "ruleExecutor must not be null");
        this.scoreCalculator = Objects.requireNonNull(scoreCalculator, "scoreCalculator must not be null");
        this.pluginLoader = Objects.requireNonNull(pluginLoader, "pluginLoader must not be null");
    }

    /**
     * Executes a full analysis synchronously and returns the complete report.
     */
    public AnalysisReport analyze(PrismGuardConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration must not be null");
        return analyzeWithProgress(configuration, NoOpProgressListener.INSTANCE);
    }

    /**
     * Executes a full analysis synchronously, reporting progress via the supplied listener.
     */
    public AnalysisReport analyzeWithProgress(PrismGuardConfiguration configuration,
                                               AnalysisProgressListener listener) {
        Objects.requireNonNull(configuration, "configuration must not be null");
        Objects.requireNonNull(listener, "listener must not be null");

        Path projectRoot = configuration.getProjectRoot();
        log.info("Starting Prism Guard analysis for project at: {}", projectRoot);
        Instant startedAt = Instant.now();

        // 0. Load and initialise plugins
        List<PrismGuardPlugin> plugins = pluginLoader.loadPlugins();
        pluginLoader.initializeAll(plugins, configuration);

        // 1. Scan
        listener.onScanStarted(projectRoot);
        log.debug("Scanning project sources...");
        ProjectModel project = scanner.scan(configuration);
        log.info("Scan complete: {} module(s), {} class(es)",
                project.getModules().size(), countClasses(project));
        listener.onScanCompleted(project);

        // 2. Build context
        RuleContext context = new DefaultRuleContext(project, configuration);

        // 3. Execute all rules with per-rule progress callbacks
        log.debug("Executing rules...");
        List<RuleResult> results = executeRulesWithProgress(context, listener);

        // 4. Aggregate findings
        List<Finding> allFindings = new ArrayList<>();
        for (RuleResult result : results) {
            if (result.findings() != null) {
                allFindings.addAll(result.findings());
            }
        }
        log.info("Rule execution complete: {} rule(s) executed, {} finding(s) produced",
                results.size(), allFindings.size());

        // 5. Calculate scores
        log.debug("Calculating scores...");
        Score overallScore = scoreCalculator.calculateOverallScore(results, configuration);
        Map<RuleCategory, Score> categoryScores = scoreCalculator.calculateCategoryScores(results, configuration);
        Map<String, Score> moduleScores = scoreCalculator.calculateModuleScores(project, allFindings, configuration);

        // 6. Build project summary from the scanned project
        ProjectSummary summary = project.getSummary();

        // 7. Calculate total duration
        Duration totalDuration = Duration.between(startedAt, Instant.now());

        // 8. Assemble report
        AnalysisReport report = AnalysisReport.builder()
                .overallScore(overallScore)
                .categoryScores(categoryScores)
                .moduleScores(moduleScores)
                .allFindings(List.copyOf(allFindings))
                .ruleResults(List.copyOf(results))
                .projectSummary(summary)
                .totalDuration(totalDuration)
                .analyzedAt(startedAt)
                .prismGuardVersion(VERSION)
                .failOnScoreBelow(configuration.getFailOnScoreBelow())
                .build();

        log.info("Analysis finished. Overall score: {}", overallScore);
        listener.onAnalysisCompleted(report);

        // 9. Shutdown plugins
        pluginLoader.shutdownAll(plugins);

        return report;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private List<RuleResult> executeRulesWithProgress(RuleContext context,
                                                       AnalysisProgressListener listener) {
        List<RuleResult> results;
        try {
            results = ruleExecutor.executeAll(context);
        } catch (Exception e) {
            log.error("Fatal error during rule execution", e);
            listener.onError("rule-executor", e);
            return List.of();
        }

        int total = results.size();
        for (int i = 0; i < total; i++) {
            RuleResult result = results.get(i);
            String ruleId = (result.ruleConfig() != null) ? result.ruleConfig().getId() : "unknown-" + i;

            if (!result.succeeded() && result.errorMessage() != null) {
                log.warn("Rule {} reported an error: {}", ruleId, result.errorMessage());
                listener.onError(ruleId, new RuntimeException(result.errorMessage()));
            } else {
                listener.onRuleCompleted(result);
            }
        }

        return results;
    }

    private int countClasses(ProjectModel project) {
        return project.getModules().stream()
                .flatMap(m -> m.getFiles().stream())
                .mapToInt(f -> f.getClasses().size())
                .sum();
    }

    // -----------------------------------------------------------------------
    // No-op listener
    // -----------------------------------------------------------------------

    private enum NoOpProgressListener implements AnalysisProgressListener {
        INSTANCE;

        @Override public void onScanStarted(Path projectRoot) {}
        @Override public void onScanCompleted(ProjectModel project) {}
        @Override public void onRuleCompleted(RuleResult result) {}
        @Override public void onAnalysisCompleted(AnalysisReport report) {}
        @Override public void onError(String ruleId, Exception error) {
            LoggerFactory.getLogger(AnalysisEngine.class)
                    .warn("Rule '{}' failed: {}", ruleId, error.getMessage());
        }
    }
}
