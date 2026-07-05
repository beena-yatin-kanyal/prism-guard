package io.prismguard.test;

import io.prismguard.api.config.PrismGuardConfiguration;
import io.prismguard.api.model.ProjectModel;
import io.prismguard.api.rule.RuleContext;
import io.prismguard.core.context.DefaultRuleContext;
import io.prismguard.core.scanner.JavaProjectScanner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a {@link RuleContext} for use in rule unit tests.
 *
 * <p>Two entry points are provided:
 * <ul>
 *   <li>{@link #forSource(String...)} — accepts one or more inline Java source snippets,
 *       writes them to a temporary directory, scans that directory, and returns a builder.</li>
 *   <li>{@link #forProject(Path)} — points directly at an existing directory on disk.</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * RuleContext ctx = TestContextBuilder
 *     .forSource("""
 *         package com.example;
 *         public class FooService { }
 *         """)
 *     .build();
 * }</pre>
 */
public final class TestContextBuilder {

    // Counter used to generate unique file names when the class name cannot be inferred.
    private static final AtomicInteger SNIPPET_COUNTER = new AtomicInteger(0);

    // Simple heuristic: look for "class Foo", "interface Foo", "enum Foo", "record Foo".
    private static final Pattern TYPE_NAME_PATTERN =
            Pattern.compile("(?:public\\s+)?(?:abstract\\s+)?(?:class|interface|enum|record|@interface)\\s+(\\w+)");

    // -------------------------------------------------------------------------
    // Public factory methods
    // -------------------------------------------------------------------------

    /**
     * Builds a {@link RuleContext} from one or more inline Java source snippets.
     *
     * <p>Each snippet is written to a temp directory under
     * {@code src/main/java/<derived-package-path>/<TypeName>.java}.
     * The class/interface/enum/record name is inferred from the source text; if inference
     * fails a unique name such as {@code Snippet1.java} is used instead.
     *
     * @param javaSources one or more Java source strings (must be valid Java)
     * @return a {@link Builder} ready to produce a {@link RuleContext}
     */
    public static Builder forSource(String... javaSources) {
        Path tempRoot;
        try {
            tempRoot = Files.createTempDirectory("prism-guard-test-");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create temp directory for test sources", e);
        }

        for (String source : javaSources) {
            writeSourceSnippet(tempRoot, source);
        }

        return new Builder(tempRoot, true);
    }

    /**
     * Builds a {@link RuleContext} by scanning an existing project directory.
     *
     * @param projectRoot path to the root of the project (or module) to analyse
     * @return a {@link Builder} ready to produce a {@link RuleContext}
     */
    public static Builder forProject(Path projectRoot) {
        return new Builder(projectRoot, false);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Writes a single Java source snippet to the temp directory tree.
     * The file is placed under {@code <tempRoot>/src/main/java/<package-path>/<TypeName>.java}.
     */
    private static void writeSourceSnippet(Path tempRoot, String source) {
        // Attempt to extract the package name
        String packageName = extractPackageName(source);
        // Attempt to extract the primary type name
        String typeName = extractTypeName(source);
        if (typeName == null) {
            typeName = "Snippet" + SNIPPET_COUNTER.incrementAndGet();
        }

        // Build the target directory: tempRoot/src/main/java/<package/path>
        Path targetDir = tempRoot.resolve("src").resolve("main").resolve("java");
        if (!packageName.isEmpty()) {
            for (String segment : packageName.split("\\.")) {
                targetDir = targetDir.resolve(segment);
            }
        }

        try {
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(typeName + ".java");
            Files.writeString(targetFile, source);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to write test source snippet '" + typeName + ".java'", e);
        }
    }

    /** Extracts the declared package from a Java source string (best-effort). */
    private static String extractPackageName(String source) {
        Pattern pkg = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
        Matcher m = pkg.matcher(source);
        return m.find() ? m.group(1) : "";
    }

    /** Extracts the primary top-level type name from a Java source string (best-effort). */
    private static String extractTypeName(String source) {
        Matcher m = TYPE_NAME_PATTERN.matcher(source);
        return m.find() ? m.group(1) : null;
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /**
     * Fluent builder that configures and materialises a {@link RuleContext}.
     */
    public static final class Builder {

        private final Path projectRoot;
        private final boolean isTempDir;
        private List<String> sourceDirectories = List.of("src/main/java");

        private Builder(Path projectRoot, boolean isTempDir) {
            this.projectRoot = projectRoot;
            this.isTempDir = isTempDir;
        }

        /**
         * Overrides the source directories scanned within the project root.
         * Defaults to {@code ["src/main/java"]}.
         *
         * @param sourceDirs one or more relative paths (e.g. {@code "src/main/java"})
         * @return this builder
         */
        public Builder withSourceDirectories(String... sourceDirs) {
            this.sourceDirectories = List.of(sourceDirs);
            return this;
        }

        /**
         * Builds and returns a fully-initialised {@link RuleContext}.
         *
         * <p>The project root is scanned with {@link JavaProjectScanner} using the
         * configured source directories, and the resulting {@link ProjectModel} is
         * wrapped in a {@link DefaultRuleContext}.
         *
         * @return a ready-to-use {@link RuleContext}
         */
        public RuleContext build() {
            PrismGuardConfiguration config = PrismGuardConfiguration.builder()
                    .projectRoot(projectRoot)
                    .sourceDirectories(sourceDirectories)
                    .build();

            JavaProjectScanner scanner = new JavaProjectScanner();
            ProjectModel project = scanner.scan(config);

            return new DefaultRuleContext(project, config);
        }
    }
}
