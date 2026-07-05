package io.prismguard.api.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The complete parsed representation of a Java project (single- or multi-module).
 */
public final class ProjectModel {

    private final String name;
    private final Path root;
    private final List<ModuleModel> modules;
    private final ProjectSummary summary;
    private final List<JavaFileModel> allFilesCache;
    private final List<ClassModel> allClassesCache;

    public ProjectModel(String name, Path root, List<ModuleModel> modules, ProjectSummary summary) {
        this.name = name;
        this.root = root;
        this.modules = modules == null ? List.of() : List.copyOf(modules);
        this.summary = summary;
        this.allFilesCache = this.modules.stream()
                .flatMap(m -> m.getFiles().stream())
                .toList();
        this.allClassesCache = this.modules.stream()
                .flatMap(ModuleModel::allClasses)
                .toList();
    }

    public String getName() {
        return name;
    }

    public Path getRoot() {
        return root;
    }

    public List<ModuleModel> getModules() {
        return modules;
    }

    public ProjectSummary getSummary() {
        return summary;
    }

    /**
     * Returns a stream of every {@link JavaFileModel} across all modules.
     * Backed by a pre-computed cache — O(1) per call, safe for repeated use.
     */
    public Stream<JavaFileModel> allFiles() {
        return allFilesCache.stream();
    }

    /**
     * Returns a stream of every {@link ClassModel} across all modules.
     * Backed by a pre-computed cache — O(1) per call, safe for repeated use.
     */
    public Stream<ClassModel> allClasses() {
        return allClassesCache.stream();
    }

    /**
     * Finds a class by its fully-qualified name.
     */
    public Optional<ClassModel> findClass(String fqn) {
        if (fqn == null) return Optional.empty();
        return allClasses().filter(c -> fqn.equals(c.getFullyQualifiedName())).findFirst();
    }

    /**
     * Returns all files whose package starts with {@code packagePrefix}.
     */
    public List<JavaFileModel> filesInPackage(String packagePrefix) {
        return allFiles()
                .filter(f -> f.isInPackage(packagePrefix))
                .toList();
    }

    @Override
    public String toString() {
        return "ProjectModel{name='" + name + "', modules=" + modules.size() + '}';
    }
}
