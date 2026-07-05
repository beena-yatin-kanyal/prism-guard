# prism-guard-test

Test utilities for writing unit tests against Prism Guard analyzer rules. Provides a fluent context builder and JUnit assertions.

---

## Responsibilities

- Construct `RuleContext` instances from inline Java source snippets without requiring a full project scan
- Provide assertion helpers for verifying `Finding` results in unit tests

---

## Key Classes

| Class | Description |
|-------|-------------|
| `TestContextBuilder` | Fluent builder that parses inline Java source strings with JavaParser and assembles a `RuleContext` suitable for passing to any `@Check` method. Supports configuring rule parameters. |
| `FindingAssert` | JUnit assertion utilities for `List<Finding>`. Provides methods to assert finding count, rule IDs, severities, and that specific files or line numbers are flagged. |

---

## Usage

```java
@Test
void detectsFieldInjection() {
    RuleContext context = new TestContextBuilder()
        .withSource("""
            @Service
            public class MyService {
                @Autowired
                private MyRepo repo;
            }
            """)
        .build();

    List<Finding> findings = new FieldInjectionRule().check(context);

    FindingAssert.assertThat(findings)
        .hasSize(1)
        .hasSeverity(FindingSeverity.HIGH);
}
```

---

## Dependencies

- `prism-guard-api`
- `prism-guard-core`
- JavaParser Core
- Spring Boot Starter Test (JUnit 5, AssertJ)
