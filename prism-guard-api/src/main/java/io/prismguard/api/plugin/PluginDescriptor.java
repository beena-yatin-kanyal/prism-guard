package io.prismguard.api.plugin;

/**
 * Metadata that uniquely identifies and describes a Prism Guard plugin.
 */
public record PluginDescriptor(
        String id,
        String name,
        String version,
        String description,
        String author,
        String url
) {
}
