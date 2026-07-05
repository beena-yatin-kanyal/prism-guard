package io.prismguard.core.engine;

import java.lang.reflect.Method;

/**
 * A validated, ready-to-invoke analyzer registered in {@link AnalyzerRegistry}.
 *
 * <p>Built once at startup: the {@link #checkMethod} has already been located,
 * verified present, and made accessible. Any analyzer class that lacks a
 * {@link io.prismguard.api.annotation.Check} method fails fast at registry
 * construction rather than at first analysis run.
 *
 * @param type        the {@link io.prismguard.api.annotation.Analyzer#type()} value
 * @param instance    the constructed analyzer object
 * @param checkMethod the {@code @Check}-annotated method, accessible
 */
record AnalyzerInstance(String type, Object instance, Method checkMethod) {
}
