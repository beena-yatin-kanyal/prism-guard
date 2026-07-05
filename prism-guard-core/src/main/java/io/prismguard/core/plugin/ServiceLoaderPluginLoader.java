package io.prismguard.core.plugin;

import io.prismguard.api.config.PrismGuardConfiguration;
import io.prismguard.api.plugin.PrismGuardPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Discovers and loads {@link PrismGuardPlugin} implementations via two mechanisms:
 * <ol>
 *   <li><b>Classpath</b>: standard {@link ServiceLoader} lookup using
 *       {@code META-INF/services/io.prismguard.api.plugin.PrismGuardPlugin}</li>
 *   <li><b>File-system</b>: any {@code *.jar} files found in a {@code plugins/}
 *       directory next to the running executable (or working directory).</li>
 * </ol>
 *
 * <p>Rule contribution from plugins is automatic: plugin JARs that include
 * {@link io.prismguard.api.annotation.Analyzer}-annotated classes and a
 * {@code META-INF/prismguard/analyzers} service file are picked up by
 * {@link io.prismguard.core.engine.AnalyzerRegistry} at startup via classpath scanning.
 */
@Component
public class ServiceLoaderPluginLoader {

    private static final Logger log = LoggerFactory.getLogger(ServiceLoaderPluginLoader.class);

    /**
     * Loads all available plugins from the classpath and the {@code plugins/} directory,
     * then calls {@link PrismGuardPlugin#initialize} on each.
     */
    public List<PrismGuardPlugin> loadPlugins() {
        List<PrismGuardPlugin> plugins = new ArrayList<>();
        plugins.addAll(loadFromClasspath());
        plugins.addAll(loadFromDirectory(resolvePluginsDirectory()));
        log.info("Loaded {} plugin(s) in total", plugins.size());
        return plugins;
    }

    List<PrismGuardPlugin> loadFromClasspath() {
        List<PrismGuardPlugin> plugins = new ArrayList<>();
        ServiceLoader<PrismGuardPlugin> loader = ServiceLoader.load(PrismGuardPlugin.class,
                Thread.currentThread().getContextClassLoader());
        for (PrismGuardPlugin plugin : loader) {
            log.info("Discovered classpath plugin: {} v{}",
                    plugin.descriptor().name(), plugin.descriptor().version());
            plugins.add(plugin);
        }
        return plugins;
    }

    List<PrismGuardPlugin> loadFromDirectory(Path pluginsDir) {
        List<PrismGuardPlugin> plugins = new ArrayList<>();

        if (pluginsDir == null || !Files.isDirectory(pluginsDir)) {
            log.debug("No plugins directory found at '{}'", pluginsDir);
            return plugins;
        }

        List<Path> jarFiles;
        try (var stream = Files.list(pluginsDir)) {
            jarFiles = stream
                    .filter(p -> p.toString().endsWith(".jar"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            log.warn("Failed to list plugin JARs in '{}': {}", pluginsDir, e.getMessage());
            return plugins;
        }

        for (Path jar : jarFiles) {
            try {
                URL jarUrl = jar.toUri().toURL();
                URLClassLoader jarLoader = new URLClassLoader(
                        new URL[]{jarUrl},
                        Thread.currentThread().getContextClassLoader());

                ServiceLoader<PrismGuardPlugin> loader = ServiceLoader.load(PrismGuardPlugin.class, jarLoader);
                for (PrismGuardPlugin plugin : loader) {
                    log.info("Loaded plugin '{}' v{} from JAR: {}",
                            plugin.descriptor().name(), plugin.descriptor().version(), jar.getFileName());
                    plugins.add(plugin);
                }
            } catch (Exception e) {
                log.warn("Failed to load plugin from JAR '{}': {}", jar, e.getMessage());
            }
        }

        return plugins;
    }

    /**
     * Calls {@link PrismGuardPlugin#initialize(io.prismguard.api.plugin.PluginContext)} on each plugin.
     * Failures are logged at WARN level and do not abort initialization of remaining plugins.
     */
    public void initializeAll(List<PrismGuardPlugin> plugins, PrismGuardConfiguration configuration) {
        for (PrismGuardPlugin plugin : plugins) {
            try {
                plugin.initialize(new DefaultPluginContext(configuration, plugin.getClass().getClassLoader()));
                log.info("Initialized plugin: {} v{}", plugin.descriptor().name(), plugin.descriptor().version());
            } catch (Exception e) {
                log.warn("Failed to initialize plugin '{}': {}", plugin.descriptor().name(), e.getMessage());
            }
        }
    }

    /**
     * Calls {@link PrismGuardPlugin#shutdown()} on each plugin.
     * Failures are logged at WARN level and do not abort shutdown of remaining plugins.
     */
    public void shutdownAll(List<PrismGuardPlugin> plugins) {
        for (PrismGuardPlugin plugin : plugins) {
            try {
                plugin.shutdown();
            } catch (Exception e) {
                log.warn("Error during shutdown of plugin '{}': {}", plugin.descriptor().name(), e.getMessage());
            }
        }
    }

    /**
     * Resolves the {@code plugins/} directory relative to the current working directory.
     * When running as a native executable, this will be next to the binary.
     */
    private Path resolvePluginsDirectory() {
        Path workingDir = Path.of(System.getProperty("user.dir", "."));
        Path pluginsDir = workingDir.resolve("plugins");
        if (Files.isDirectory(pluginsDir)) {
            return pluginsDir;
        }
        String codeSourcePath = ServiceLoaderPluginLoader.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath();
        Path codeDir = Path.of(codeSourcePath).getParent();
        if (codeDir != null) {
            Path sibling = codeDir.resolve("plugins");
            if (Files.isDirectory(sibling)) {
                return sibling;
            }
        }
        return pluginsDir;
    }
}
