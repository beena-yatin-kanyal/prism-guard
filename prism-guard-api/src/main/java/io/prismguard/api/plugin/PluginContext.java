package io.prismguard.api.plugin;

import io.prismguard.api.config.PrismGuardConfiguration;

import java.nio.file.Path;

/**
 * Runtime context passed to a plugin during its {@link PrismGuardPlugin#initialize(PluginContext)} phase.
 */
public interface PluginContext {

    /**
     * Returns the root directory of the project being analysed.
     */
    Path projectRoot();

    /**
     * Returns the active Prism Guard configuration.
     */
    PrismGuardConfiguration configuration();

    /**
     * Returns the class loader used to load this plugin's JAR.
     * Useful when a plugin needs to load resources from its own JAR.
     */
    ClassLoader pluginClassLoader();
}
