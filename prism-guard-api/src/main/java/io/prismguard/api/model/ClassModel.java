package io.prismguard.api.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Represents a parsed Java class, interface, enum, annotation, or record.
 */
public final class ClassModel {

    private static final Set<String> SPRING_COMPONENT_ANNOTATIONS = Set.of(
            "Component", "Service", "Repository", "Controller", "RestController",
            "Configuration", "ControllerAdvice", "RestControllerAdvice",
            "SpringBootApplication", "EnableAutoConfiguration"
    );

    private final String simpleName;
    private final String fullyQualifiedName;
    private final String packageName;
    private final ClassType classType;
    private final List<String> annotations;
    private final List<MethodModel> methods;
    private final List<FieldModel> fields;
    private final List<String> implementedInterfaces;
    private final String superClass;
    private final Path sourceFile;
    private final int beginLine;
    private final int endLine;
    private final boolean isAbstract;
    private final boolean isPublic;

    public ClassModel(
            String simpleName,
            String fullyQualifiedName,
            String packageName,
            ClassType classType,
            List<String> annotations,
            List<MethodModel> methods,
            List<FieldModel> fields,
            List<String> implementedInterfaces,
            String superClass,
            Path sourceFile,
            int beginLine,
            int endLine,
            boolean isAbstract,
            boolean isPublic) {
        this.simpleName = simpleName;
        this.fullyQualifiedName = fullyQualifiedName;
        this.packageName = packageName;
        this.classType = classType;
        this.annotations = annotations == null ? List.of() : List.copyOf(annotations);
        this.methods = methods == null ? List.of() : List.copyOf(methods);
        this.fields = fields == null ? List.of() : List.copyOf(fields);
        this.implementedInterfaces = implementedInterfaces == null ? List.of() : List.copyOf(implementedInterfaces);
        this.superClass = superClass;
        this.sourceFile = sourceFile;
        this.beginLine = beginLine;
        this.endLine = endLine;
        this.isAbstract = isAbstract;
        this.isPublic = isPublic;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public String getPackageName() {
        return packageName;
    }

    public ClassType getClassType() {
        return classType;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public List<MethodModel> getMethods() {
        return methods;
    }

    public List<FieldModel> getFields() {
        return fields;
    }

    public List<String> getImplementedInterfaces() {
        return implementedInterfaces;
    }

    public String getSuperClass() {
        return superClass;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    public int getBeginLine() {
        return beginLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public boolean isPublic() {
        return isPublic;
    }

    /**
     * Returns {@code true} if this class carries the given annotation.
     * Matches by simple name or fully-qualified name.
     */
    public boolean hasAnnotation(String name) {
        if (name == null) return false;
        String simpleName = simpleNameOf(name);
        return annotations.stream()
                .anyMatch(a -> a.equals(name) || simpleNameOf(a).equals(simpleName));
    }

    /**
     * Returns {@code true} if this class is a Spring-managed component
     * ({@code @Component}, {@code @Service}, {@code @Repository}, etc.).
     */
    public boolean isSpringComponent() {
        return annotations.stream()
                .map(ClassModel::simpleNameOf)
                .anyMatch(SPRING_COMPONENT_ANNOTATIONS::contains);
    }

    /**
     * Returns the number of lines spanned by this type declaration.
     */
    public int lineCount() {
        return Math.max(0, endLine - beginLine + 1);
    }

    public int methodCount() {
        return methods.size();
    }

    public int fieldCount() {
        return fields.size();
    }

    private static String simpleNameOf(String nameOrFqn) {
        int dot = nameOrFqn.lastIndexOf('.');
        return dot >= 0 ? nameOrFqn.substring(dot + 1) : nameOrFqn;
    }

    @Override
    public String toString() {
        return "ClassModel{fqn='" + fullyQualifiedName + "', type=" + classType
                + ", lines=" + beginLine + "-" + endLine + '}';
    }
}
