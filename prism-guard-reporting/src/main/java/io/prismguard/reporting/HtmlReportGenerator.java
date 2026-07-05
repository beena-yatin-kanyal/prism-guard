package io.prismguard.reporting;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import io.prismguard.api.model.AnalysisReport;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.ProjectSummary;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.model.Score;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates a self-contained HTML dashboard from the analysis report using FreeMarker templates.
 * Output file: {@code <outputDirectory>/prismguard-report.html}.
 * The generated file has no external dependencies — all CSS and JS are inlined.
 */
@Component
public class HtmlReportGenerator implements ReportGenerator {

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter FILE_TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());

    private final Configuration freemarkerCfg;

    public HtmlReportGenerator() {
        freemarkerCfg = new Configuration(Configuration.VERSION_2_3_33);
        freemarkerCfg.setTemplateLoader(new ClassTemplateLoader(HtmlReportGenerator.class, "/templates/"));
        freemarkerCfg.setDefaultEncoding(StandardCharsets.UTF_8.name());
        freemarkerCfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarkerCfg.setLogTemplateExceptions(false);
        freemarkerCfg.setWrapUncheckedExceptions(true);
    }

    @Override
    public String format() {
        return "html";
    }

    @Override
    public Path generate(AnalysisReport report, Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        String timestamp = report.getAnalyzedAt() != null
                ? FILE_TS_FMT.format(report.getAnalyzedAt())
                : FILE_TS_FMT.format(java.time.Instant.now());
        Path outputFile = outputDirectory.resolve("prismguard-report-" + timestamp + ".html");

        Map<String, Object> model = buildTemplateModel(report);

        try {
            Template template = freemarkerCfg.getTemplate("report.ftl");
            try (Writer writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
                template.process(model, writer);
            }
        } catch (TemplateException e) {
            throw new IOException("FreeMarker template processing failed: " + e.getMessage(), e);
        }
        return outputFile;
    }

    // -------------------------------------------------------------------------
    // Template model assembly
    // -------------------------------------------------------------------------

    private Map<String, Object> buildTemplateModel(AnalysisReport report) {
        Map<String, Object> model = new HashMap<>();

        model.put("report", report);
        model.put("overallScorePercent", (int) Math.round(
                report.getOverallScore() != null ? report.getOverallScore().getValue() : 0.0));
        model.put("overallGrade", report.getOverallScore() != null ? report.getOverallScore().grade() : "F");
        model.put("generatedAt", report.getAnalyzedAt() != null
                ? DISPLAY_FMT.format(report.getAnalyzedAt()) : "—");

        int threshold = report.getFailOnScoreBelow();

        // Category scores as a sorted list of maps for easy FTL iteration
        model.put("categoryScoreList", buildCategoryScoreList(report.getCategoryScores(), threshold));

        // Module scores as a sorted list of maps
        model.put("moduleScoreList", buildModuleScoreList(report.getModuleScores(), threshold));

        // Findings enriched with display-friendly fields
        model.put("findingsList", buildFindingsList(report.getAllFindings()));

        // Summary counts per severity
        model.put("severitySummary", buildSeveritySummary(report.findingsBySeverity()));

        // Total findings count
        int totalFindings = report.getAllFindings() != null ? report.getAllFindings().size() : 0;
        model.put("totalFindings", totalFindings);

        // Critical count
        int criticalCount = report.criticalFindings() != null ? report.criticalFindings().size() : 0;
        model.put("criticalCount", criticalCount);

        // Categories analyzed
        int categoriesAnalyzed = report.getCategoryScores() != null ? report.getCategoryScores().size() : 0;
        model.put("categoriesAnalyzed", categoriesAnalyzed);

        // Duration display string
        model.put("durationDisplay", report.getTotalDuration() != null
                ? formatDuration(report.getTotalDuration())
                : "—");

        // Expose project summary fields as flat values for easier FTL access
        ProjectSummary ps = report.getProjectSummary();
        if (ps != null) {
            model.put("projectName",    ps.projectName() != null ? ps.projectName() : "");
            model.put("projectRoot",    ps.projectRoot() != null ? ps.projectRoot().toString() : "");
            model.put("totalFiles",     ps.totalFiles());
            model.put("totalClasses",   ps.totalClasses());
            model.put("totalMethods",   ps.totalMethods());
            model.put("totalLines",     ps.totalLines());
            model.put("isMultiModule",  ps.isMultiModule());
            model.put("hasSpringBoot",  ps.hasSpringBoot());
            model.put("frameworks",     ps.detectedFrameworks() != null
                    ? String.join(", ", ps.detectedFrameworks()) : "");
        } else {
            model.put("projectName",   "");
            model.put("projectRoot",   "");
            model.put("totalFiles",    0);
            model.put("totalClasses",  0);
            model.put("totalMethods",  0);
            model.put("totalLines",    0);
            model.put("isMultiModule", false);
            model.put("hasSpringBoot", false);
            model.put("frameworks",    "");
        }

        return model;
    }

    private List<Map<String, Object>> buildCategoryScoreList(Map<RuleCategory, Score> categoryScores, int threshold) {
        if (categoryScores == null) return List.of();
        return categoryScores.entrySet().stream()
                .sorted(Map.Entry.<RuleCategory, Score>comparingByValue(
                        (a, b) -> Double.compare(b.getValue(), a.getValue())))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name",    e.getKey().getDisplayName());
                    m.put("key",     e.getKey().name());
                    m.put("value",   e.getValue().getValue());
                    m.put("grade",   e.getValue().grade());
                    m.put("label",   e.getValue().label());
                    m.put("passing", e.getValue().isPassing(threshold));
                    m.put("barPct",  (int) Math.round(e.getValue().getValue()));
                    return m;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildModuleScoreList(Map<String, Score> moduleScores, int threshold) {
        if (moduleScores == null) return List.of();
        return moduleScores.entrySet().stream()
                .sorted(Map.Entry.<String, Score>comparingByValue(
                        (a, b) -> Double.compare(b.getValue(), a.getValue())))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name",    e.getKey());
                    m.put("value",   e.getValue().getValue());
                    m.put("grade",   e.getValue().grade());
                    m.put("label",   e.getValue().label());
                    m.put("passing", e.getValue().isPassing(threshold));
                    return m;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildFindingsList(List<Finding> findings) {
        if (findings == null) return List.of();
        return findings.stream()
                .sorted((a, b) -> Integer.compare(a.getSeverity().getOrder(), b.getSeverity().getOrder()))
                .map(f -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("ruleId",          f.getRuleId() != null ? f.getRuleId() : "");
                    m.put("ruleName",        f.getRuleName() != null ? f.getRuleName() : "");
                    m.put("severity",        f.getSeverity().name());
                    m.put("severityDisplay", f.getSeverity().getDisplayName());
                    m.put("category",        f.getCategory() != null ? f.getCategory().name() : "");
                    m.put("categoryDisplay", f.getCategory() != null ? f.getCategory().getDisplayName() : "");
                    m.put("message",         escapeHtml(f.getMessage() != null ? f.getMessage() : ""));
                    m.put("recommendation",  escapeHtml(f.getRecommendation() != null ? f.getRecommendation() : ""));
                    String filePath = f.getFilePath() != null ? f.getFilePath() : "";
                    int lineNo = f.getLineNumber();
                    m.put("location",        lineNo > 0 ? filePath + ":" + lineNo : filePath);
                    m.put("hasSnippet",      f.getCodeSnippet() != null && !f.getCodeSnippet().isBlank());
                    m.put("snippet",         escapeHtml(f.getCodeSnippet() != null ? f.getCodeSnippet() : ""));
                    return m;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildSeveritySummary(Map<FindingSeverity, List<Finding>> findingsBySeverity) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (FindingSeverity sev : FindingSeverity.values()) {
            int count = findingsBySeverity.getOrDefault(sev, List.of()).size();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name",    sev.name());
            m.put("display", sev.getDisplayName());
            m.put("count",   count);
            list.add(m);
        }
        return list;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String formatDuration(java.time.Duration d) {
        long ms = d.toMillis();
        if (ms < 1000) return ms + " ms";
        return String.format("%.2f s", ms / 1000.0);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#39;");
    }
}
