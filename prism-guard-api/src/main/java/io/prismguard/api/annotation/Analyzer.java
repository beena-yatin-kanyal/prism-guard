package io.prismguard.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a reusable analysis engine that can power one or more rules defined
 * in {@code prismguard.yml}.
 *
 * <h3>Generic Analyzer Architecture</h3>
 * <p>Unlike {@link PrismRule} (which ties a class to one specific rule ID), an
 * {@code @Analyzer} class is identified only by a capability type name (e.g.
 * {@code "long-method"}). Any number of rules in {@code prismguard.yml} can reference
 * the same analyzer type via the {@code analyzer:} key and supply different parameters,
 * enabling the same detection logic to appear as multiple distinct rules across
 * different projects or rule catalogs — without changing any Java code.
 *
 * <h3>Rule classes</h3>
 * <p>An analyzer class must have:
 * <ul>
 *   <li>A public no-arg constructor.</li>
 *   <li>Exactly one method annotated with {@link Check}.</li>
 * </ul>
 *
 * <h3>Parameter access</h3>
 * <p>The {@link Check} method receives a {@link io.prismguard.api.rule.RuleContext}. To
 * read rule-specific thresholds the analyzer must call
 * {@link io.prismguard.api.rule.RuleContext#parameter(String, Object)}. The context
 * is pre-loaded with the calling rule's parameters from {@code prismguard.yml}, so the
 * analyzer never needs to know which rule ID it is executing.
 *
 * <h3>Discovery</h3>
 * <p>At compile time, {@code RuleAnnotationProcessor} detects all {@code @Analyzer}
 * classes and writes their fully-qualified names into
 * {@code META-INF/prismguard/analyzers}. At runtime, {@code AnalyzerRegistry} reads
 * every copy of this file from the classpath and builds a type-name → instance map.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Analyzer {
    /**
     * Unique analyzer capability type, e.g. {@code "long-method"} or
     * {@code "cyclomatic-complexity"}. Referenced from {@code prismguard.yml} via
     * {@code analyzer: long-method}. Use lower-case kebab-case.
     */
    String type();
}
