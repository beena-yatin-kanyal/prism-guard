package io.prismguard.api.model;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Represents a single Maven/Gradle module (or the entire project when not multi-module).
 */
public final class ModuleModel {

    private final String name;
    private final Path root;
    private final List<JavaFileModel> files;

    public ModuleModel(String name, Path root, List<JavaFileModel> files) {
        this.name = name;
        this.root = root;
        this.files = files == null ? List.of() : List.copyOf(files);
    }

    public String getName() {
        return name;
    }

    public Path getRoot() {
        return root;
    }

    public List<JavaFileModel> getFiles() {
        return files;
    }

    /**
     * Returns a stream of all {@link ClassModel} instances across every file in this module.
     */
    public Stream<ClassModel> allClasses() {
        return files.stream().flatMap(f -> f.getClasses().stream());
    }

    public int fileCount() {
        return files.size();
    }

    public int classCount() {
        return (int) allClasses().count();
    }

    @Override
    public String toString() {
        return "ModuleModel{name='" + name + "', files=" + files.size() + '}';
    }
}
