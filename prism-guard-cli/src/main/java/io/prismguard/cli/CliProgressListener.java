package io.prismguard.cli;

import io.prismguard.api.model.AnalysisReport;
import io.prismguard.api.model.ModuleModel;
import io.prismguard.api.model.ProjectModel;
import io.prismguard.api.model.RuleResult;
import io.prismguard.engine.AnalysisProgressListener;
import org.fusesource.jansi.Ansi;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link AnalysisProgressListener} that renders a live progress display in the terminal.
 * Each rule execution produces one line of output; carriage-return rewriting is used
 * during the "running" phase so that the current rule name stays in place until it finishes.
 */
public class CliProgressListener implements AnalysisProgressListener {

    private final AtomicInteger completed = new AtomicInteger(0);

    // -------------------------------------------------------------------------
    // Lifecycle callbacks
    // -------------------------------------------------------------------------

    @Override
    public void onScanStarted(Path projectRoot) {
        System.out.println(
            Ansi.ansi()
                .fgCyan().a("⟳ Scanning project: ").reset()
                .a(projectRoot.toString()));
    }

    @Override
    public void onScanCompleted(ProjectModel project) {
        // ProjectModel has conventional getters (not record accessors)
        int files   = 0;
        int classes = 0;
        if (project != null && project.getModules() != null) {
            for (ModuleModel module : project.getModules()) {
                if (module.getFiles() != null) {
                    files += module.getFiles().size();
                    for (var javaFile : module.getFiles()) {
                        if (javaFile.getClasses() != null) {
                            classes += javaFile.getClasses().size();
                        }
                    }
                }
            }
        }
        System.out.printf("%s Scan complete: %d files, %d classes%n",
            Ansi.ansi().fgGreen().a("✓").reset(),
            files,
            classes);
        System.out.println();
    }

    @Override
    public void onRuleCompleted(RuleResult result) {
        completed.incrementAndGet();
        // RuleResult is a record — use record accessor methods
        int count = result.findings() != null ? result.findings().size() : 0;
        String status = count > 0
            ? Ansi.ansi().fgYellow().a("⚠ " + count + " issue(s)").reset().toString()
            : Ansi.ansi().fgGreen().a("✓ clean").reset().toString();

        String ruleName = result.ruleConfig() != null && result.ruleConfig().getTitle() != null
                ? result.ruleConfig().getTitle()
                : (result.ruleConfig() != null ? result.ruleConfig().getId() : "unknown");
        System.out.printf("\r%-60s %s%n",
            "  " + ruleName,
            status);
    }

    @Override
    public void onAnalysisCompleted(AnalysisReport report) {
        System.out.println();
        int findingsCount = report.getAllFindings() != null ? report.getAllFindings().size() : 0;
        int rulesCount    = report.getRuleResults() != null ? report.getRuleResults().size() : 0;
        System.out.printf("%s Analysis complete. %d findings across %d rules.%n",
            Ansi.ansi().fgGreen().bold().a("✓").reset(),
            findingsCount,
            rulesCount);
        System.out.println();
    }

    @Override
    public void onError(String ruleId, Exception error) {
        System.out.printf("%n%s Rule %s failed: %s%n",
            Ansi.ansi().fgRed().a("✗").reset(),
            ruleId,
            error.getMessage());
    }
}
