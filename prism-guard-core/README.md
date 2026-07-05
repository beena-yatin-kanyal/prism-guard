# prism-guard-core

The infrastructure layer of Prism Guard. Handles configuration loading, Java source scanning, rule discovery, rule execution, score calculation, and plugin loading.

---

## Responsibilities

- Load and validate `prismguard.yml` configuration
- Scan a Java project directory tree and parse `.java` files into a `ProjectModel`
- Discover `@Analyzer` classes at runtime and build a registry
- Execute rules in parallel using Java 21 virtual threads
- Calculate overall, category-level, and module-level scores from findings
- Load third-party plugins via `ServiceLoader`

---

## Key Classes

### Configuration (`io.prismguard.core.config`)

| Class | Description |
|-------|-------------|
| `YamlConfigurationLoader` | Deserialises `prismguard.yml` into `PrismGuardConfiguration` using Jackson YAML |
| `ConfigurationValidator` | Validates rule references, severity/category enums, and parameter types; throws `ConfigurationValidationException` on failure |

### Scanner (`io.prismguard.core.scanner`)

| Class | Description |
|-------|-------------|
| `JavaProjectScanner` | Walks the project directory, skips excluded paths, parses every `.java` file with JavaParser, and assembles `ProjectModel` with `ModuleModel`, `JavaFileModel`, `ClassModel`, `MethodModel`, `FieldModel` |

### Rule Engine (`io.prismguard.core.engine`)

| Class | Description |
|-------|-------------|
| `AnalyzerRegistry` | Reads `META-INF/prismguard/analyzers` at startup and instantiates each `@Analyzer` class. Indexed by `type` string. |
| `AnalyzerInstance` | Wrapper holding an instantiated analyzer, its reflected `@Check` method, and its type name |
| `AnalyzerBasedRule` | Adapts an `AnalyzerInstance` to the `Rule` interface; invokes the `@Check` method and collects findings |
| `RuleRegistry` | Builds the set of active `Rule` objects by matching `RuleConfig` entries from the YAML against registered `AnalyzerInstance` objects |
| `RuleExecutor` | Runs all active rules concurrently with `Executors.newVirtualThreadPerTaskExecutor()` (Java 21); enforces a 30-second timeout per rule |
| `ScoreCalculator` | Applies per-severity penalty weights (CRITICAL −15, HIGH −8, MEDIUM −3, LOW −1) with per-severity caps; aggregates scores by `RuleCategory` and by module |
| `AnalyzerSupport` | Shared utility methods used by analyzer implementations |

### Compile-time (`io.prismguard.core.processor`)

| Class | Description |
|-------|-------------|
| `RuleAnnotationProcessor` | `javax.annotation.processing.AbstractProcessor` that discovers every `@Analyzer`-annotated class at compile time and writes their FQNs to `META-INF/prismguard/analyzers`. Runs automatically via the Maven compiler plugin — no manual registration needed. |

### Context (`io.prismguard.core.context`)

| Class | Description |
|-------|-------------|
| `DefaultRuleContext` | Implements `RuleContext`; wraps `ProjectModel`, `PrismGuardConfiguration`, and a rule's `parameters` map to supply typed parameter access to analyzer `@Check` methods |

### Plugin Loading (`io.prismguard.core.plugin`)

| Class | Description |
|-------|-------------|
| `ServiceLoaderPluginLoader` | Loads `PrismGuardPlugin` implementations from the classpath via `ServiceLoader`; calls `initialize` and `shutdown` lifecycle methods |
| `DefaultPluginContext` | Concrete `PluginContext` passed to plugins during initialisation |

---

## Dependencies

- `prism-guard-api`
- Spring Context (for dependency injection wiring)
- JavaParser Core (AST parsing)
- Jackson + jackson-dataformat-yaml (config loading)

---

## Extension Points

- Drop any `@Analyzer` class on the classpath. `RuleAnnotationProcessor` registers it at compile time; `AnalyzerRegistry` picks it up at runtime.
- Implement `PrismGuardPlugin` and register via `META-INF/services/io.prismguard.api.plugin.PrismGuardPlugin` to bundle a third-party rule library.
