# prism-guard-analyzers

All built-in analysis rules. Contains 60+ `@Analyzer` implementations organised by concern. Each analyzer is self-contained; it is auto-discovered at compile time and requires no registration beyond the `prismguard.yml` rule entry.

---

## Responsibilities

- Implement every built-in rule as an `@Analyzer` + `@Check` class
- Depend only on `prism-guard-api` and `prism-guard-core`
- Produce `Finding` objects that describe the violation, location, and remediation

---

## Analyzer Catalogue

### Java Quality (`io.prismguard.analyzers.java`) — 15 rules

| Analyzer type | Rule ID | What it detects |
|---------------|---------|-----------------|
| `public-field` | JAVA001 | Non-constant public instance fields |
| `long-method` | JAVA002 | Methods exceeding a configurable line threshold |
| `high-cyclomatic-complexity` | JAVA003 | Methods with cyclomatic complexity above threshold |
| `empty-exception-handling` | JAVA004 | Empty or comment-only catch blocks |
| `utility-class-design` | JAVA005 | Utility classes that can be instantiated |
| `mutable-static-field` | JAVA006 | Non-final static fields |
| `final-class-public-constructor` | JAVA007 | `final` classes with public constructors |
| `primitive-obsession` | JAVA008 | Classes with excessive primitive fields |
| `magic-number` | JAVA009 | Inline numeric literals (configurable exclusions) |
| `god-class` | JAVA010 | Classes exceeding size/complexity thresholds |
| `abstract-class-no-abstract-methods` | JAVA012 | Abstract classes with no abstract methods |
| `test-class-without-tests` | JAVA013 | Test-named classes with no `@Test` methods |
| `serializable-without-serial-version-uid` | JAVA014 | `Serializable` classes missing `serialVersionUID` |
| `excessive-method-parameters` | JAVA011 | Methods with more than `maxParams` parameters |
| `overloaded-method-excess` | JAVA015 | Method names overloaded more than `maxOverloads` times |

### Spring Boot (`io.prismguard.analyzers.spring`) — 17 rules

| Analyzer type | Rule ID | What it detects |
|---------------|---------|-----------------|
| `field-injection` | SPRING001 | `@Autowired` on fields instead of constructors |
| `missing-transactional` | SPRING002 | Service methods that modify data without `@Transactional` |
| `controller-business-logic` | SPRING003 | Business logic in `@Controller`/`@RestController` |
| `repository-in-service` | SPRING004 | Direct repository usage bypassing the service layer |
| `component-scan-broad` | SPRING005 | Overly broad `@ComponentScan` base packages |
| `missing-request-mapping-validation` | SPRING006 | `@RequestMapping` handlers lacking input validation |
| `missing-error-handler` | SPRING007 | Controllers without `@ExceptionHandler` or `@ControllerAdvice` |
| `excessive-autowiring` | SPRING008 | Classes injecting more than a configurable number of dependencies |
| `transactional-on-private-method` | SPRING009 | `@Transactional` on private methods (Spring proxy limitation) |
| `hardcoded-credentials` | SPRING010 | Hardcoded passwords/credentials in Spring configuration |
| `deprecated-spring-pattern` | SPRING011 | Usage of deprecated Spring patterns |
| `spring-boot-actuator-missing` | SPRING012 | Missing Spring Boot Actuator dependency |
| `async-method-not-public` | SPRING013 | Non-public `@Async` methods (ignored by Spring proxy) |
| `configuration-properties-no-validation` | SPRING014 | `@ConfigurationProperties` classes without `@Validated` |
| `rest-endpoint-returns-void` | SPRING015 | REST handler methods returning `void` |
| `bean-method-not-in-configuration` | SPRING016 | `@Bean` methods outside a `@Configuration` class (lite-mode) |
| `http-session-in-rest-controller` | SPRING017 | `HttpSession` injection in `@RestController` (breaks stateless design) |

### JPA / Hibernate (`io.prismguard.analyzers.jpa`) — 6 rules

| Analyzer type | Rule ID | What it detects |
|---------------|---------|-----------------|
| `entity-missing-generated-value` | JPA001 | `@Entity` classes without `@GeneratedValue` on the ID |
| `entity-missing-equals-hashcode` | JPA002 | `@Entity` classes missing `equals`/`hashCode` |
| `entity-public-field` | JPA003 | Public fields on `@Entity` classes |
| `transactional-on-entity` | JPA004 | `@Transactional` directly on `@Entity` classes |
| `entity-not-serializable` | JPA005 | `@Entity` classes not implementing `Serializable` |
| `one-to-many-without-batch-size` | JPA006 | `@OneToMany` without `@BatchSize` (N+1 risk) |

### SOLID Principles (`io.prismguard.analyzers.architecture`) — 5 rules

| Analyzer type | Rule IDs | What it detects |
|---------------|----------|-----------------|
| `single-responsibility` | SOLID001 | Classes with high complexity indicating multiple responsibilities |
| `open-closed-principle` | SOLID002 | Classes that may require modification rather than extension |
| `liskov-substitution` | SOLID003 | Potential LSP violations in inheritance hierarchies |
| `interface-segregation` | SOLID004 | Large interfaces imposing unused method implementations |
| `dependency-inversion` | SOLID005 | High-level modules depending on low-level implementations |

### Architecture & Design (`io.prismguard.analyzers.architecture`) — 8 rules

| Analyzer type | Rule ID | What it detects |
|---------------|---------|-----------------|
| `layering-violation` | ARCH001 | Cross-layer imports (e.g. controller importing repository) |
| `circular-dependency` | ARCH002 | Circular inter-package dependencies |
| `god-package` | ARCH003 | Packages with excessively many classes |
| `naming-convention` | ARCH004 | Classes violating naming conventions for their layer/type |
| `feature-envy` | ARCH005 | Methods that use more of another class than their own |
| `util-class-in-domain` | ARCH006 | Utility classes placed in domain packages |
| `presentation-layer-returning-domain` | ARCH007 | Controllers returning domain/entity objects directly |

### Clean Architecture (`io.prismguard.analyzers.cleanarch`) — 4 rules

| Analyzer type | Rule ID | What it detects |
|---------------|---------|-----------------|
| `anemic-domain-model` | CLEAN001 | Domain classes with only accessor methods and no behaviour |
| `domain-depends-on-framework` | CLEAN002 | Domain/model classes importing Spring or JPA framework packages |
| `service-without-interface` | CLEAN003 | `@Service` classes not implementing any business interface |
| `service-layer-calling-controller` | CLEAN004 | Service/application classes importing controller packages |

### REST API Design (`io.prismguard.analyzers.rest`) — 3 rules

| Analyzer type | Rule ID | What it detects |
|---------------|---------|-----------------|
| `endpoint-missing-response-entity` | REST001 | REST endpoints not returning `ResponseEntity` |
| `large-controller` | REST002 | `@RestController` with more than `maxEndpoints` handler methods |
| `controller-method-without-mapping` | REST003 | Public methods in `@RestController` with no HTTP mapping annotation |

### Dependency Injection (`io.prismguard.analyzers.di`) — 3 rules

| Analyzer type | Rule ID | What it detects |
|---------------|---------|-----------------|
| `application-context-direct-usage` | DI001 | Spring components holding direct references to `ApplicationContext` or `BeanFactory` |
| `lazy-injection-detected` | DI002 | Injected fields annotated with `@Lazy` |
| `optional-dependency-injection` | DI003 | Injected fields of type `Optional<T>` |

---

## Dependencies

- `prism-guard-api`
- `prism-guard-core`
- Spring Context
- Google Guava
- JavaParser Core

---

## Extension Points

To add an analyzer:

1. Create a class annotated with `@Analyzer(type = "my-type")` in any sub-package.
2. Add a `@Check` method accepting `RuleContext` and returning `List<Finding>`.
3. Add a rule entry in `prismguard-example.yml` referencing the new `analyzer` type.

No further registration is required.
