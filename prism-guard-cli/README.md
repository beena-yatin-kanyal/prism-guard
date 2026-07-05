# prism-guard-cli

Spring Boot CLI application. The runnable artifact of Prism Guard. Provides `analyze`, `init`, and `list-rules` commands via Picocli.

---

## Responsibilities

- Bootstrap Spring Boot and initialise the analysis engine
- Parse command-line arguments with Picocli
- Drive `AnalysisEngine` for project scanning and rule execution
- Render progress and results to the terminal using ANSI colour via Jansi
- Delegate report generation to `prism-guard-reporting`
- Write a `prismguard-example.yml` template when `init` is invoked

---

## Key Classes

| Class | Description |
|-------|-------------|
| `PrismGuardApplication` | Spring Boot entry point. Bootstraps the application context and enables Jansi ANSI output. |
| `PrismGuardCommand` | Root Picocli command. Declares version info and groups sub-commands. |
| `AnalyzeCommand` | `analyze <path>` sub-command. Loads config, runs `AnalysisEngine`, prints summary, triggers reports, and exits with appropriate code. |
| `InitCommand` | `init` sub-command. Copies `prismguard-example.yml` from classpath to the current directory. |
| `ListRulesCommand` | `list-rules` sub-command. Iterates `AnalyzerRegistry` and prints all registered analyzer types. |
| `CliProgressListener` | Implements `AnalysisProgressListener`. Prints scan/rule progress lines to stdout with ANSI colours. |

---

## Building

```bash
mvn clean package -pl prism-guard-cli -am -q
# produces: prism-guard-cli/target/prism-guard-cli-1.0.0.jar
```

## Running

```bash
java --enable-native-access=ALL-UNNAMED \
  -jar prism-guard-cli/target/prism-guard-cli-1.0.0.jar \
  analyze /path/to/project --config prismguard.yml --format html
```

## Exit Codes

| Code | Condition |
|------|-----------|
| 0 | Analysis succeeded and all pass criteria met |
| 1 | Analysis error or config validation failure |
| 2 | `--fail-on-critical` triggered or `--fail-on-score-below` threshold not met |

---

## Resources

| File | Description |
|------|-------------|
| `prismguard-example.yml` | Full template written by `init` command; also loaded as fallback when no config file is found |
| `application.yml` | Spring Boot settings (logging levels, Picocli integration) |
| `logback.xml` | Logging configuration |

---

## Dependencies

- All Prism Guard modules
- Spring Boot 3.3.4 (starter)
- Picocli 4.7.6 + `picocli-spring-boot-starter`
- Jansi 2.4.1
- Logback (runtime)
