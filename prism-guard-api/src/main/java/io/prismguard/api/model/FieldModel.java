package io.prismguard.api.model;

import java.util.List;
import java.util.Set;

/**
 * Represents a field declaration within a Java class.
 */
public record FieldModel(
        String name,
        String type,
        List<String> annotations,
        boolean isPrivate,
        boolean isStatic,
        boolean isFinal
) {

    private static final Set<String> INJECTION_ANNOTATIONS = Set.of(
            "Autowired", "Inject", "Resource", "Value",
            "org.springframework.beans.factory.annotation.Autowired",
            "javax.inject.Inject",
            "jakarta.inject.Inject",
            "javax.annotation.Resource",
            "jakarta.annotation.Resource",
            "org.springframework.beans.factory.annotation.Value"
    );

    public FieldModel {
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }

    /**
     * Returns {@code true} if this field carries the given annotation (simple name or FQN).
     */
    public boolean hasAnnotation(String name) {
        if (name == null) return false;
        String simpleName = simpleNameOf(name);
        return annotations.stream()
                .anyMatch(a -> a.equals(name) || simpleNameOf(a).equals(simpleName));
    }

    /**
     * Returns {@code true} if this field is injection-annotated
     * ({@code @Autowired}, {@code @Inject}, {@code @Resource}, {@code @Value}).
     */
    public boolean isInjected() {
        return annotations.stream()
                .anyMatch(a -> INJECTION_ANNOTATIONS.contains(a) || INJECTION_ANNOTATIONS.contains(simpleNameOf(a)));
    }

    private static String simpleNameOf(String nameOrFqn) {
        int dot = nameOrFqn.lastIndexOf('.');
        return dot >= 0 ? nameOrFqn.substring(dot + 1) : nameOrFqn;
    }
}
