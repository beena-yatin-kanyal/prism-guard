package io.prismguard.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as the analysis entry point within a {@link PrismRule}-annotated class.
 *
 * <p>The annotated method must have signature:
 * <pre>{@code List<Finding> methodName(RuleContext context)}</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Check {
    /** Optional label describing what this check detects, shown in verbose output. */
    String description() default "";
}
