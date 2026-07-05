package io.prismguard.core.plugin;

import io.prismguard.api.config.PrismGuardConfiguration;
import io.prismguard.api.plugin.PluginContext;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Canonical implementation of {@link PluginContext} passed to each plugin during
 * {@link io.prismguard.api.plugin.PrismGuardPlugin#initialize(PluginContext)}.
 */
public final class DefaultPluginContext implements PluginContext {

    private final PrismGuardConfiguration configuration;
    private final ClassLoader pluginClassLoader;

    public DefaultPluginContext(PrismGuardConfiguration configuration, ClassLoader pluginClassLoader) {
        this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
        this.pluginClassLoader = pluginClassLoader != null
                ? pluginClassLoader
                : Thread.currentThread().getContextClassLoader();
    }

    @Override
    public Path projectRoot() {
        return configuration.getProjectRoot();
    }

    @Override
    public PrismGuardConfiguration configuration() {
        return configuration;
    }

    @Override
    public ClassLoader pluginClassLoader() {
        return pluginClassLoader;
    }
}
