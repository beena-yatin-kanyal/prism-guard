package io.prismguard.core.engine;

import io.prismguard.api.config.PrismGuardConfiguration;
import io.prismguard.api.model.ClassModel;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.ModuleModel;
import io.prismguard.api.model.ProjectModel;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.model.RuleResult;
import io.prismguard.api.model.Score;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calculates quality scores from rule execution results.
 *
 * <p>The penalty model is configured via the {@code scoring:} section of
 * {@code prismguard.yml} and exposed through {@link PrismGuardConfiguration.ScoringConfig}.
 * When absent, defaults matching the original hardcoded values are used:
 * <pre>
 *   CRITICAL : -15 per finding, capped at -45
 *   HIGH     :  -8 per finding, capped at -32
 *   MEDIUM   :  -3 per finding, capped at -15
 *   LOW      :  -1 per finding, capped at  -5
 *   INFO     :   0 (no deduction)
 * </pre>
 */
@Component
public class ScoreCalculator {

    private static final Logger log = LoggerFactory.getLogger(ScoreCalculator.class);

    /**
     * Computes the overall project score from all rule results.
     */
    public Score calculateOverallScore(List<RuleResult> results, PrismGuardConfiguration configuration) {
        List<Finding> allFindings = results.stream()
                .flatMap(r -> r.findings().stream())
                .toList();
        Score score = calculateScoreFromFindings(allFindings, configuration.getScoringConfig());
        log.debug("Overall score: {} from {} findings", score, allFindings.size());
        return score;
    }

    /**
     * Computes per-category scores from all rule results.
     *
     * <p>Every category that had at least one rule executed is included in the result,
     * defaulting to {@link Score#PERFECT} when no findings were produced. This allows
     * consumers to distinguish "category not run" (absent from map) from "category ran
     * clean" (present with score 100).
     */
    public Map<RuleCategory, Score> calculateCategoryScores(List<RuleResult> results,
                                                             PrismGuardConfiguration configuration) {
        PrismGuardConfiguration.ScoringConfig sc = configuration.getScoringConfig();
        Map<RuleCategory, Score> scores = new EnumMap<>(RuleCategory.class);

        // Seed every executed category with a perfect score
        for (RuleResult result : results) {
            if (result.ruleConfig() == null) continue;
            RuleCategory cat = AnalyzerSupport.parseCategory(
                    result.ruleConfig().getId(), result.ruleConfig().getCategory());
            scores.putIfAbsent(cat, Score.PERFECT);
        }

        // Overwrite with penalty scores for categories that have findings
        Map<RuleCategory, List<Finding>> findingsByCategory = results.stream()
                .flatMap(r -> r.findings().stream())
                .filter(f -> f.getCategory() != null)
                .collect(Collectors.groupingBy(Finding::getCategory));

        findingsByCategory.forEach((category, findings) ->
                scores.put(category, calculateScoreFromFindings(findings, sc)));

        return scores;
    }

    /**
     * Computes per-module scores. A module's score is derived from findings whose
     * {@code filePath} falls within the module's root directory.
     */
    public Map<String, Score> calculateModuleScores(ProjectModel project, List<Finding> findings,
                                                     PrismGuardConfiguration configuration) {
        Map<String, Score> moduleScores = new HashMap<>();
        PrismGuardConfiguration.ScoringConfig sc = configuration.getScoringConfig();

        for (ModuleModel module : project.getModules()) {
            String moduleRootStr = module.getRoot().toString();

            List<Finding> moduleFindings = findings.stream()
                    .filter(f -> {
                        String fp = f.getFilePath();
                        return fp != null && fp.startsWith(moduleRootStr);
                    })
                    .toList();

            moduleScores.put(module.getName(), calculateScoreFromFindings(moduleFindings, sc));
        }

        return moduleScores;
    }

    /**
     * Computes a score from a list of findings using the supplied penalty model.
     */
    Score calculateScoreFromFindings(List<Finding> findings, PrismGuardConfiguration.ScoringConfig sc) {
        if (findings == null || findings.isEmpty()) {
            return Score.PERFECT;
        }

        Map<FindingSeverity, Long> countBySeverity = findings.stream()
                .collect(Collectors.groupingBy(Finding::getSeverity, Collectors.counting()));

        double totalPenalty = 0.0;

        long criticalCount = countBySeverity.getOrDefault(FindingSeverity.CRITICAL, 0L);
        totalPenalty += Math.min(criticalCount * sc.getCriticalPenalty(), sc.getCriticalCap());

        long highCount = countBySeverity.getOrDefault(FindingSeverity.HIGH, 0L);
        totalPenalty += Math.min(highCount * sc.getHighPenalty(), sc.getHighCap());

        long mediumCount = countBySeverity.getOrDefault(FindingSeverity.MEDIUM, 0L);
        totalPenalty += Math.min(mediumCount * sc.getMediumPenalty(), sc.getMediumCap());

        long lowCount = countBySeverity.getOrDefault(FindingSeverity.LOW, 0L);
        totalPenalty += Math.min(lowCount * sc.getLowPenalty(), sc.getLowCap());

        // INFO carries no penalty

        return Score.of(100.0 - totalPenalty);
    }
}
