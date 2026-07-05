package io.prismguard.api.plugin;

/**
 * Entry point for a Prism Guard extension plugin.
 *
 * <p>Plugins are discovered via {@link java.util.ServiceLoader} using the service file
 * {@code META-INF/services/io.prismguard.api.plugin.PrismGuardPlugin}.
 *
 * <h3>Contributing rules</h3>
 * <p>Rule contribution is automatic — no explicit contribution method is needed.
 * Include {@link io.prismguard.api.annotation.Analyzer}-annotated classes and a
 * {@code META-INF/prismguard/analyzers} service file in the plugin JAR.
 * {@link io.prismguard.core.engine.AnalyzerRegistry} discovers and registers them
 * at application startup. Users reference the analyzer types from their project's
 * {@code prismguard.yml}.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>{@link #initialize(PluginContext)} — called once before any analysis</li>
 *   <li>{@link #shutdown()} — called after analysis is complete</li>
 * </ol>
 */
public interface PrismGuardPlugin {

    /**
     * Returns metadata describing this plugin.
     */
    PluginDescriptor descriptor();

    /**
     * Called once before analysis begins. Implementations may use this hook
     * to load resources, validate preconditions, or warm up caches.
     */
    default void initialize(PluginContext context) {
    }

    /**
     * Called after analysis completes. Implementations should release any
     * resources acquired during {@link #initialize(PluginContext)}.
     */
    default void shutdown() {
    }
}
