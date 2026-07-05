# Contributing to Prism Guard

We welcome contributions! Whether it's a bug report, new analyzer, or feature improvement — every bit helps. Please follow these guidelines.

## Issues

Found a bug or have a feature idea? [Open an issue](https://github.com/yatinbatra/prism-guard/issues) before starting work. Check that one doesn't already exist, then describe the problem or proposal clearly.

## Development Setup

**Prerequisites:**
- Java 21 (LTS)
- Maven 3.9+
- IDE: IntelliJ IDEA or VS Code with Java extension

**Build the project:**
```bash
mvn clean package -pl prism-guard-cli -am
```

This produces the runnable CLI JAR at `prism-guard-cli/target/prism-guard-cli-1.0.0.jar`.

**Run locally:**
```bash
./prism-guard analyze /path/to/your/java/project
```

## Pull Requests

1. [Open an issue](https://github.com/yatinbatra/prism-guard/issues) describing the bug or feature. Ask a maintainer to assign it to you.
2. Fork the repo and create a branch for your work.
3. Write your changes with tests (see [Testing](#testing)).
4. Ensure all tests pass: `mvn test`
5. Open a Pull Request referencing the issue number (e.g. `Fixes #42`).
6. A maintainer will review, give feedback, and merge when ready.

## Adding a New Analyzer

Analyzers live in `prism-guard-analyzers/src/main/java/io/prismguard/analyzers/`.

1. Create a class annotated with `@Analyzer`:

```java
@Analyzer(
    id = "my-rule-id",
    name = "My Rule Name",
    description = "What this rule checks",
    category = "java",
    severity = Severity.MEDIUM
)
public class MyAnalyzer implements Rule {

    @Check
    public List<Finding> analyze(RuleContext context) {
        // use context.getProjectModel() to traverse classes
        return findings;
    }
}
```

2. Place it under the appropriate category package (`java/`, `spring/`, `jpa/`, `architecture/`, `cleanarch/`, `rest/`, `di/`).
3. Rules are discovered automatically at compile-time via `@Analyzer` + `@Check` — no manual registration needed.
4. Add a test (see [Testing](#testing)).

## Testing

Use the `prism-guard-test` module utilities for analyzer unit tests:

```java
RuleContext context = TestContextBuilder.fromSource("""
    public class MyClass {
        // inline Java source to test against
    }
""").build();

List<Finding> findings = new MyAnalyzer().analyze(context);

FindingAssert.assertThat(findings)
    .hasSize(1)
    .first().hasRuleId("my-rule-id");
```

Run all tests:
```bash
mvn test
```

## Module Structure

| Module | Purpose |
|--------|---------|
| `prism-guard-api` | Public API — annotations, model classes, `Rule` interface. Zero external dependencies. |
| `prism-guard-core` | Infrastructure — YAML config loading, AST scanning (JavaParser), rule engine, scoring. |
| `prism-guard-engine` | Orchestrator — wires scan → execute → score → report. |
| `prism-guard-analyzers` | 60+ built-in rule implementations. |
| `prism-guard-reporting` | Report generators (HTML via FreeMarker). |
| `prism-guard-cli` | Spring Boot + Picocli CLI entry point. |
| `prism-guard-test` | Test utilities for rule authors. |

Avoid adding dependencies to `prism-guard-api` — it must remain dependency-free.

## Code Style

- Follow existing patterns in each module.
- IntelliJ code style settings are in `.idea/codeStyles/`.
- No auto-formatter PRs — keep changes focused on the feature or fix.
