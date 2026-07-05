package io.prismguard.api.model;

import java.util.List;

/**
 * Represents a method declaration within a Java class.
 */
public final class MethodModel {

    private final String name;
    private final String returnType;
    private final List<String> parameterTypes;
    private final List<String> annotations;
    private final boolean isPublic;
    private final boolean isStatic;
    private final boolean isAbstract;
    private final int beginLine;
    private final int endLine;
    private final int cyclomaticComplexity;

    public MethodModel(
            String name,
            String returnType,
            List<String> parameterTypes,
            List<String> annotations,
            boolean isPublic,
            boolean isStatic,
            boolean isAbstract,
            int beginLine,
            int endLine,
            int cyclomaticComplexity) {
        this.name = name;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes == null ? List.of() : List.copyOf(parameterTypes);
        this.annotations = annotations == null ? List.of() : List.copyOf(annotations);
        this.isPublic = isPublic;
        this.isStatic = isStatic;
        this.isAbstract = isAbstract;
        this.beginLine = beginLine;
        this.endLine = endLine;
        this.cyclomaticComplexity = cyclomaticComplexity;
    }

    public String getName() {
        return name;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public int getBeginLine() {
        return beginLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getCyclomaticComplexity() {
        return cyclomaticComplexity;
    }

    public int lineCount() {
        return Math.max(0, endLine - beginLine + 1);
    }

    /**
     * Returns {@code true} if this method carries the given annotation (simple name or FQN).
     */
    public boolean hasAnnotation(String name) {
        if (name == null) return false;
        String simpleName = simpleNameOf(name);
        return annotations.stream()
                .anyMatch(a -> a.equals(name) || simpleNameOf(a).equals(simpleName));
    }

    /**
     * Returns {@code true} if this method is annotated with {@code @Transactional}
     * (either Spring or JEE variant).
     */
    public boolean isTransactional() {
        return hasAnnotation("Transactional")
                || hasAnnotation("org.springframework.transaction.annotation.Transactional")
                || hasAnnotation("javax.transaction.Transactional")
                || hasAnnotation("jakarta.transaction.Transactional");
    }

    private static String simpleNameOf(String nameOrFqn) {
        int dot = nameOrFqn.lastIndexOf('.');
        return dot >= 0 ? nameOrFqn.substring(dot + 1) : nameOrFqn;
    }

    @Override
    public String toString() {
        return "MethodModel{name='" + name + "', returnType='" + returnType
                + "', lines=" + beginLine + "-" + endLine + ", cc=" + cyclomaticComplexity + '}';
    }
}
