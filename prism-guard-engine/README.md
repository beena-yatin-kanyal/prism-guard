# prism-guard-engine

Top-level analysis orchestrator. Coordinates scanning, rule execution, scoring, and report assembly into a single `AnalysisReport`.

---

## Responsibilities

- Expose a single entry point (`AnalysisEngine`) that drives the full analysis pipeline
- Delegate each pipeline stage to the appropriate core service
- Emit progress events via `AnalysisProgressListener`
- Cache previous scan results for incremental re-analysis

---

## Key Classes

| Class | Description |
|-------|-------------|
| `AnalysisEngine` | Main entry point. Accepts a `PrismGuardConfiguration` and a `AnalysisProgressListener`, then runs: scan → rule execution → score calculation → report assembly. Returns a complete `AnalysisReport`. |
| `AnalysisProgressListener` | Callback interface for lifecycle events: scan started/completed, each rule started/completed, analysis completed. `CliProgressListener` in the CLI module is the primary implementation. |
| `IncrementalAnalysisCache` | Stores the last `ProjectModel` scan result keyed by project path. Allows the engine to skip re-parsing unchanged files on repeated runs. |
| `EngineAutoConfiguration` | Spring Boot `@Configuration` class that wires `AnalysisEngine` as a Spring bean. |

---

## Pipeline

```
PrismGuardConfiguration
        │
        ▼
  JavaProjectScanner          (prism-guard-core)
        │ ProjectModel
        ▼
    RuleExecutor               (prism-guard-core, virtual threads)
        │ List<RuleResult>
        ▼
  ScoreCalculator              (prism-guard-core)
        │ Map<category, Score>
        ▼
  AnalysisReport assembly
```

---

## Dependencies

- `prism-guard-api`
- `prism-guard-core`
- JavaParser Core
- Jackson

---

## Extension Points

Implement `AnalysisProgressListener` to receive real-time events during analysis (useful for IDE integrations or custom dashboards).
