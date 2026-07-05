# prism-guard-api

Zero-dependency public API for Prism Guard. All other modules depend on this module; nothing here depends on them.

---

## Responsibilities

- Define the domain model (project, classes, methods, fields, findings, scores)
- Declare the rule and plugin extension contracts
- Provide the annotations used to mark analyzer classes

---

## Key Classes

### Annotations (`io.prismguard.api.annotation`)

| Class | Description |
|-------|-------------|
| `@Analyzer` | Marks a class as an analysis engine. The `type` attribute is the key used in `prismguard.yml` (e.g. `"long-method"`). |
| `@Check` | Marks the single method on an `@Analyzer` class that performs analysis. Must accept `RuleContext` and return `List<Finding>`. |

### Domain Model (`io.prismguard.api.model`)

| Class | Description |
|-------|-------------|
| `ProjectModel` | Root of the parsed project (modules, files) |
| `ModuleModel` | Single Maven/Gradle module |
| `JavaFileModel` | Parsed `.java` file with package name, imports, and class list |
| `ClassModel` | Class, interface, enum, annotation, or record with annotations, methods, fields, line info |
| `MethodModel` | Method with name, return type, parameters, cyclomatic complexity, line count |
| `FieldModel` | Field (record) with name, type, annotations, modifiers |
| `ClassType` | `CLASS`, `INTERFACE`, `ENUM`, `ANNOTATION`, `RECORD` |
| `Finding` | A single rule violation (rule ID, severity, file, line, message, recommendation) |
| `FindingSeverity` | `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO` |
| `RuleCategory` | 15+ categories: `JAVA_QUALITY`, `SPRING_BOOT`, `SECURITY`, `CLEAN_ARCHITECTURE`, etc. |
| `AnalysisReport` | Complete output: overall score, category scores, module scores, all findings |
| `Score` | Numeric score value with grade and label |
| `ProjectSummary` | High-level project stats (file count, class count, line count) |

### Rule Contracts (`io.prismguard.api.rule`)

| Class | Description |
|-------|-------------|
| `Rule` | Core interface: `ruleConfig()`, `analyze(RuleContext)`, `isApplicable(ProjectModel)` |
| `RuleContext` | Passed to every analyzer at runtime. Provides `project()`, `config()`, and `parameter(name, default)` for rule-specific YAML parameters |

### Configuration (`io.prismguard.api.config`)

| Class | Description |
|-------|-------------|
| `PrismGuardConfiguration` | Root config: source dirs, excludes, Java version, output format, fail criteria |
| `RuleConfig` | Per-rule entry: ID, title, analyzer type, severity, category, parameters, enabled flag |
| `ReportConfiguration` | Output format and output directory |

### Plugin System (`io.prismguard.api.plugin`)

| Class | Description |
|-------|-------------|
| `PrismGuardPlugin` | Extension point for third-party rule libraries. Loaded via `java.util.ServiceLoader`. |
| `PluginDescriptor` | Plugin metadata (name, version, description) |
| `PluginContext` | Context passed to `PrismGuardPlugin.initialize()` |

---

## Dependencies

None. This module has no compile-time dependencies on other Prism Guard modules or Spring.

---

## Extension Points

- Implement `PrismGuardPlugin` and register via `META-INF/services` to add a third-party rule library.
- Use `@Analyzer` + `@Check` on any class in any package — the compile-time `RuleAnnotationProcessor` in `prism-guard-core` discovers them automatically.
