package io.prismguard.core.engine;

import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.annotation.Check;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Startup registry of all {@link Analyzer}-annotated classes, keyed by their
 * {@link Analyzer#type()} value.
 *
 * <h3>Discovery</h3>
 * <p>At compile time {@code RuleAnnotationProcessor} writes the fully-qualified class
 * names of all {@code @Analyzer} classes into
 * {@code META-INF/prismguard/analyzers}. This registry reads every copy of that file
 * from the classpath (one per rule module JAR), instantiates each class via its
 * public no-arg constructor, and builds the type-name → instance map.
 *
 * <h3>Usage</h3>
 * <p>{@link RuleRegistry} injects this registry and calls {@link #findByType(String)}
 * when it creates {@link AnalyzerBasedRule} instances for each YAML-defined rule.
 */
@Component
public class AnalyzerRegistry {

    private static final Logger log = LoggerFactory.getLogger(AnalyzerRegistry.class);
    private static final String ANALYZER_SERVICE_FILE = "META-INF/prismguard/analyzers";

    private final Map<String, AnalyzerInstance> analyzersByType;

    public AnalyzerRegistry() {
        Map<String, AnalyzerInstance> map = new HashMap<>();
        loadAnalyzers(map);
        this.analyzersByType = Collections.unmodifiableMap(map);
        log.info("AnalyzerRegistry: loaded {} analyzer type(s): {}", map.size(), map.keySet());
    }

    private void loadAnalyzers(Map<String, AnalyzerInstance> map) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = AnalyzerRegistry.class.getClassLoader();

        try {
            Enumeration<URL> resources = cl.getResources(ANALYZER_SERVICE_FILE);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {

                    reader.lines()
                            .map(String::trim)
                            .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                            .forEach(className -> registerAnalyzer(className, map));

                } catch (IOException e) {
                    log.warn("Could not read analyzer service file '{}': {}", url, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Failed to enumerate analyzer service files: {}", e.getMessage(), e);
        }
    }

    private void registerAnalyzer(String className, Map<String, AnalyzerInstance> map) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) cl = AnalyzerRegistry.class.getClassLoader();
            Class<?> cls = cl.loadClass(className);
            Analyzer meta = cls.getAnnotation(Analyzer.class);
            if (meta == null) {
                log.warn("Class '{}' listed in {} but not annotated with @Analyzer — skipping",
                        className, ANALYZER_SERVICE_FILE);
                return;
            }
            String type = meta.type();
            if (map.containsKey(type)) {
                log.warn("Duplicate analyzer type '{}' from '{}' — skipping", type, className);
                return;
            }
            Method checkMethod = Arrays.stream(cls.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(Check.class))
                    .findFirst()
                    .orElse(null);
            if (checkMethod == null) {
                log.error("Analyzer class '{}' (type '{}') has no @Check method — skipping at startup",
                        className, type);
                return;
            }
            checkMethod.setAccessible(true);
            Object instance = cls.getDeclaredConstructor().newInstance();
            map.put(type, new AnalyzerInstance(type, instance, checkMethod));
            log.debug("Registered analyzer type '{}' → {}", type, className);
        } catch (Exception e) {
            log.error("Failed to load analyzer class '{}': {}", className, e.getMessage(), e);
        }
    }

    /**
     * Returns the validated {@link AnalyzerInstance} for the given type name,
     * or empty if not registered.
     */
    public Optional<AnalyzerInstance> findByType(String type) {
        return Optional.ofNullable(analyzersByType.get(type));
    }

    /** Returns an unmodifiable view of all registered analyzers. */
    public Map<String, AnalyzerInstance> getAll() {
        return analyzersByType;
    }

    public int size() {
        return analyzersByType.size();
    }
}
