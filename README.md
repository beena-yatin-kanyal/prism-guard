# Prism Guard

A static architecture analysis tool for Java and Spring Boot projects. Prism Guard scans source code, evaluates it against configurable rules, and produces scored reports with actionable findings.

---

## Architecture Overview

```
prism-guard-cli          CLI entry point (Spring Boot + Picocli)
      │
prism-guard-engine       Orchestration: scan → execute → score → report
      │
prism-guard-core         Rule engine, registry, scanner, score calculator
      │
prism-guard-api          Public interfaces, domain model, extension annotations
      │
prism-guard-analyzers    60+ built-in rule implementations
      │
prism-guard-reporting    Report generators (HTML via FreeMarker)
      │
prism-guard-test         Test utilities for rule authors
```

Each module depends only on modules below it. Analyzers and reporters are discovered at runtime; no hardcoded registration is needed.

---

## Module Overview

| Module | Description |
|--------|-------------|
| `prism-guard-api` | Zero-dependency public API: annotations, model classes, rule/plugin contracts |
| `prism-guard-core` | YAML config loading, AST scanning, rule registry, score calculation, annotation processor |
| `prism-guard-engine` | Top-level analysis orchestrator |
| `prism-guard-analyzers` | Built-in analyzers covering Java quality, Spring Boot, JPA, SOLID, architecture, REST, DI |
| `prism-guard-cli` | Spring Boot CLI application (`analyze`, `init`, `list-rules` commands) |
| `prism-guard-reporting` | HTML report generation |
| `prism-guard-test` | `TestContextBuilder` and `FindingAssert` for unit-testing rules |

---

## Installation

**Requirements:** Java 21, Maven 3.9+

```bash
git clone <repo>
cd primsa-guard
mvn clean package -pl prism-guard-cli -am -q
```

The runnable JAR is produced at `prism-guard-cli/target/prism-guard-cli-1.0.0.jar`.

---

## CLI Usage

```bash
java --enable-native-access=ALL-UNNAMED \
  -jar prism-guard-cli/target/prism-guard-cli-1.0.0.jar \
  <command> [options]
```

### Commands

| Command | Description |
|---------|-------------|
| `analyze <path>` | Analyse a Java project at the given path |
| `init` | Write a `prismguard-example.yml` template to the current directory |
| `list-rules` | Print all available analyzer types |

### `analyze` options

| Option | Default | Description |
|--------|---------|-------------|
| `--config`, `-c` | `prismguard.yml` | Path to config file |
| `--format`, `-f` | `html` | Output format (`html`) |
| `--output`, `-o` | `./prismguard-reports` | Output directory |
| `--fail-on-critical` | false | Exit non-zero if CRITICAL findings exist |
| `--fail-on-score-below` | 0 | Exit non-zero if overall score is below threshold |

**Example:**

```bash
java --enable-native-access=ALL-UNNAMED \
  -jar prism-guard-cli/target/prism-guard-cli-1.0.0.jar \
  analyze /path/to/my-spring-project \
  --config prismguard.yml \
  --format html \
  --output ./reports
```

---

## `prismguard.yml` Configuration

Place `prismguard.yml` in the project root you are analysing. Use `prism-guard init` to generate a starting template.

```yaml
sourceDirectories:
  - src/main/java

excludePatterns:
  - "**/generated/**"
  - "**/target/**"
  - "**/*Test.java"

javaVersion: 21
outputFormat: html
outputDirectory: ./prismguard-reports
failOnCritical: false
failOnScoreBelow: 0

rules:
  JAVA001:
    analyzer: public-field
    title: "Public Field"
    severity: MEDIUM
    category: JAVA_QUALITY
    enabled: true

  SPRING001:
    analyzer: field-injection
    title: "Field Injection"
    severity: HIGH
    category: SPRING_BOOT
    enabled: true
    recommendation: "Use constructor injection instead."

  JAVA011:
    analyzer: excessive-method-parameters
    title: "Excessive Method Parameters"
    severity: MEDIUM
    category: JAVA_QUALITY
    enabled: true
    parameters:
      maxParams: 7
```

Each rule entry maps a rule ID to an `analyzer` type. Rules are optional: only rules listed in the config are executed.

---

## Contributing

### Adding an Analyzer

1. Create a class annotated with `@Analyzer(type = "my-rule-type")` in `prism-guard-analyzers`.
2. Add a `@Check`-annotated method that accepts `RuleContext` and returns `List<Finding>`.
3. Add a corresponding rule entry in `prismguard-example.yml`.

The compile-time `RuleAnnotationProcessor` automatically registers new analyzers — no additional wiring is required.

```java
@Analyzer(type = "my-rule-type")
public class MyRule {
    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();
        context.project().allClasses()
            .filter(/* ... */)
            .forEach(cls -> findings.add(Finding.builder()
                .ruleId("MY001")
                .ruleName("MY001")
                .severity(FindingSeverity.MEDIUM)
                .category(RuleCategory.JAVA_QUALITY)
                .message("...")
                .build()));
        return findings;
    }
}
```

### Adding a Report Format

Implement `ReportGenerator` and annotate the class with `@Component`. Spring Boot auto-detection picks it up at startup.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.3.4 |
| AST parsing | JavaParser 3.26.2 |
| CLI | Picocli 4.7.6 |
| Templates | FreeMarker 2.3.33 |
| Config | Jackson + jackson-dataformat-yaml |
| Console | Jansi 2.4.1 |
| Build | Maven (multi-module) |
