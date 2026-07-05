package io.prismguard.api.model;

import java.nio.file.Path;
import java.util.List;

/**
 * Represents a parsed {@code .java} source file and its contained type declarations.
 */
public final class JavaFileModel {

    private final Path filePath;
    private final String packageName;
    private final List<ClassModel> classes;
    private final List<String> imports;
    private final int lineCount;

    public JavaFileModel(
            Path filePath,
            String packageName,
            List<ClassModel> classes,
            List<String> imports,
            int lineCount) {
        this.filePath = filePath;
        this.packageName = packageName == null ? "" : packageName;
        this.classes = classes == null ? List.of() : List.copyOf(classes);
        this.imports = imports == null ? List.of() : List.copyOf(imports);
        this.lineCount = lineCount;
    }

    public Path getFilePath() {
        return filePath;
    }

    public String getPackageName() {
        return packageName;
    }

    public List<ClassModel> getClasses() {
        return classes;
    }

    public List<String> getImports() {
        return imports;
    }

    public int getLineCount() {
        return lineCount;
    }

    /**
     * Returns {@code true} if this file's package starts with {@code pkg}.
     */
    public boolean isInPackage(String pkg) {
        if (pkg == null || pkg.isBlank()) return false;
        return packageName.equals(pkg) || packageName.startsWith(pkg + ".");
    }

    /**
     * Returns {@code true} if the import list contains the given fully-qualified name.
     */
    public boolean hasImport(String importFqn) {
        if (importFqn == null) return false;
        return imports.contains(importFqn);
    }

    /**
     * Returns {@code true} if any class in this file carries the given annotation.
     */
    public boolean hasAnnotation(String annotationName) {
        if (annotationName == null) return false;
        return classes.stream().anyMatch(c -> c.hasAnnotation(annotationName));
    }

    @Override
    public String toString() {
        return "JavaFileModel{file='" + filePath + "', package='" + packageName
                + "', classes=" + classes.size() + '}';
    }
}
