# prism-guard-reporting

Report generation for Prism Guard. Currently produces a self-contained HTML dashboard using FreeMarker templates.

---

## Responsibilities

- Define the `ReportGenerator` strategy interface
- Implement HTML report generation via FreeMarker
- Provide `ReportingService` to select the correct generator based on configured output format
- Expose Spring Boot autoconfiguration so generators are picked up automatically

---

## Key Classes

| Class | Description |
|-------|-------------|
| `ReportGenerator` | Strategy interface with two methods: `format()` (returns the format key, e.g. `"html"`) and `generate(AnalysisReport, Path)` (writes the report and returns the output file path). |
| `HtmlReportGenerator` | Implements `ReportGenerator` for format `"html"`. Configures FreeMarker, loads `templates/report.ftl` from the classpath, assembles a template model from `AnalysisReport`, and writes `prismguard-report-<timestamp>.html` to the output directory. The generated file has no external dependencies — all CSS and JS are inlined. |
| `ReportingService` | Orchestrator injected with all `ReportGenerator` beans. Selects the generator matching the configured `outputFormat`, creates the output directory, and delegates to `generate()`. Logs a warning if no matching generator is found. |
| `ReportingAutoConfiguration` | Spring Boot `@Configuration` class that registers `HtmlReportGenerator` and `ReportingService` as beans. |

---

## HTML Report Contents

The generated HTML dashboard includes:

- Overall score and grade
- Per-category score bars with pass/fail status
- Per-module score table
- Findings table sorted by severity (rule ID, severity, category, message, file location, code snippet, recommendation)
- Severity summary counts (CRITICAL / HIGH / MEDIUM / LOW / INFO)
- Project summary (file count, class count, method count, line count, detected frameworks)
- Analysis timestamp and duration

Output filename pattern: `prismguard-report-<yyyy-MM-dd_HH-mm-ss>.html`

---

## Dependencies

- `prism-guard-api`
- `prism-guard-core`
- FreeMarker 2.3.33
- Jackson (for model serialisation in templates)
- Spring Boot (autoconfiguration)

---

## Extension Points

Add a new report format by implementing `ReportGenerator` and annotating the class with `@Component`. `ReportingService` discovers all registered generators by format key at startup.

```java
@Component
public class JsonReportGenerator implements ReportGenerator {
    @Override public String format() { return "json"; }
    @Override public Path generate(AnalysisReport report, Path outputDirectory) throws IOException { ... }
}
```

Enable the new format in `prismguard.yml`:

```yaml
outputFormat: json
```
