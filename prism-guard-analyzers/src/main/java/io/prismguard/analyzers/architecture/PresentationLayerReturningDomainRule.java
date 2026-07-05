package io.prismguard.analyzers.architecture;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.ClassModel;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.model.MethodModel;
import io.prismguard.api.rule.RuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ARCH004 — Domain Entity Leaked to Presentation Layer.
 *
 * Finds REST controller methods whose return type is directly a JPA {@code @Entity}
 * or {@code @Table}-annotated class. Controllers must map entities to DTOs before
 * returning them to clients.
 */
@Analyzer(type = "presentation-layer-returning-domain")
public class PresentationLayerReturningDomainRule {

    private static final Logger log = LoggerFactory.getLogger(PresentationLayerReturningDomainRule.class);

    private static final Set<String> CONTROLLER_ANNOTATIONS = Set.of(
            "RestController", "Controller",
            "org.springframework.web.bind.annotation.RestController",
            "org.springframework.stereotype.Controller"
    );

    private static final Set<String> ENTITY_ANNOTATIONS = Set.of(
            "Entity", "Table",
            "javax.persistence.Entity", "jakarta.persistence.Entity",
            "javax.persistence.Table", "jakarta.persistence.Table"
    );

    /** Return types that are known safe wrappers — skip them. */
    private static final Set<String> SAFE_WRAPPER_TYPES = Set.of(
            "ResponseEntity", "String", "void", "Void",
            "ModelAndView", "View", "HttpEntity", "HttpHeaders"
    );

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> isController(cls))
                .forEach(cls -> analyzeController(cls, context, findings));

        return findings;
    }

    private boolean isController(ClassModel cls) {
        return CONTROLLER_ANNOTATIONS.stream().anyMatch(cls::hasAnnotation);
    }

    private void analyzeController(ClassModel cls, RuleContext context, List<Finding> findings) {
        for (MethodModel method : cls.getMethods()) {
            if (!method.isPublic()) {
                continue;
            }
            String returnType = method.getReturnType();
            if (returnType == null || returnType.isBlank()) {
                continue;
            }

            String rawReturnType = stripGenericWrapper(returnType);
            if (SAFE_WRAPPER_TYPES.contains(rawReturnType)) {
                continue;
            }

            // Check if the return type is an entity in the project
            if (isEntityType(rawReturnType, context)) {
                int lineNumber = cls.getBeginLine(); // Method-level line not available via MethodModel

                findings.add(Finding.builder()
                        .ruleId("ARCH004")
                        .ruleName("ARCH004")
                        .severity(FindingSeverity.HIGH)
                        .category(RuleCategory.CLEAN_ARCHITECTURE)
                        .message(String.format(
                                "Controller method '%s.%s()' returns entity '%s' directly. Use a DTO instead.",
                                cls.getSimpleName(), method.getName(), rawReturnType))
                        .recommendation("Map entities to DTOs/response objects before returning from controllers. Entities should not leak into the presentation layer.")
                        .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : "")
                        .lineNumber(lineNumber)
                        .columnNumber(0)
                        .codeSnippet(String.format("public %s %s(...) { // in @Controller %s }",
                                returnType, method.getName(), cls.getSimpleName()))
                        .build());
            }
        }
    }

    /**
     * Unwraps common generic container types to get the underlying payload type.
     * e.g. {@code ResponseEntity<User>} → {@code User},
     *      {@code List<Order>} → {@code Order},
     *      {@code User} → {@code User}.
     */
    private String stripGenericWrapper(String returnType) {
        String type = returnType.trim();

        // Handle ResponseEntity<T>, List<T>, Optional<T>, Collection<T>, etc.
        int genericStart = type.indexOf('<');
        if (genericStart < 0) {
            return type;
        }
        String outerType = type.substring(0, genericStart);
        String innerType = type.substring(genericStart + 1, type.length() - 1).trim();

        // Known wrappers that should be unwrapped to inspect their payload
        Set<String> wrappersToUnwrap = Set.of(
                "ResponseEntity", "Optional", "Mono", "Flux",
                "List", "Collection", "Set", "Iterable", "Page", "Slice"
        );

        if (wrappersToUnwrap.contains(outerType)) {
            // Recurse to handle nested generics like ResponseEntity<List<User>>
            return stripGenericWrapper(innerType);
        }

        return outerType;
    }

    /**
     * Returns true if the given simple type name corresponds to a class with
     * an {@code @Entity} or {@code @Table} annotation in the project.
     */
    private boolean isEntityType(String simpleTypeName, RuleContext context) {
        // First try exact FQN lookup (unlikely to match a simple name, but cheap)
        Optional<ClassModel> found = context.project().findClass(simpleTypeName);
        if (found.isPresent()) {
            return isAnnotatedAsEntity(found.get());
        }

        // Fall back to simple-name search
        return context.project().allClasses()
                .filter(c -> c.getSimpleName().equals(simpleTypeName))
                .anyMatch(this::isAnnotatedAsEntity);
    }

    private boolean isAnnotatedAsEntity(ClassModel cls) {
        return ENTITY_ANNOTATIONS.stream().anyMatch(cls::hasAnnotation);
    }
}
