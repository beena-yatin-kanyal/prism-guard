package io.prismguard.engine;

import io.prismguard.api.model.AnalysisReport;
import io.prismguard.api.model.ProjectModel;
import io.prismguard.api.model.RuleResult;

import java.nio.file.Path;

/**
 * Listener interface for receiving progress notifications during an analysis run.
 * Implementors can track rule-by-rule progress, handle errors, and react to
 * intermediate scan results without blocking the analysis pipeline.
 */
public interface AnalysisProgressListener {

    /**
     * Called when the filesystem scan phase begins.
     *
     * @param projectRoot the root directory being scanned
     */
    void onScanStarted(Path projectRoot);

    /**
     * Called after the filesystem scan phase completes and the {@link ProjectModel} is built.
     *
     * @param project the fully-populated project model
     */
    void onScanCompleted(ProjectModel project);

    /**
     * Called immediately after a rule finishes executing (whether it produced findings or not).
     *
     * @param result the result produced by the rule
     */
    void onRuleCompleted(RuleResult result);

    /**
     * Called when the entire analysis pipeline has finished and the report is ready.
     *
     * @param report the final analysis report
     */
    void onAnalysisCompleted(AnalysisReport report);

    /**
     * Called when an unrecoverable error occurs during the execution of a specific rule.
     * The engine will skip that rule and continue with remaining rules.
     *
     * @param ruleId the identifier of the rule that failed
     * @param error  the exception that caused the failure
     */
    void onError(String ruleId, Exception error);

}
