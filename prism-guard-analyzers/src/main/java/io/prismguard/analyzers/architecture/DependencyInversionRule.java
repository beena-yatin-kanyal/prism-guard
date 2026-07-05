package io.prismguard.analyzers.architecture;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.ClassModel;
import io.prismguard.api.model.ClassType;
import io.prismguard.api.model.FieldModel;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * SOLID005 — Dependency Inversion Principle Violation.
 *
 * Finds Spring beans whose injected fields depend on concrete classes (other Spring
 * beans that are not interfaces or abstract classes) rather than abstractions.
 */
@Analyzer(type = "dependency-inversion")
public class DependencyInversionRule {

    private static final Logger log = LoggerFactory.getLogger(DependencyInversionRule.class);

    /** Annotations that mark a field as injected by Spring. */
    private static final Set<String> INJECT_ANNOTATIONS = Set.of(
            "Autowired", "Inject", "Resource", "Value"
    );

    /** Annotations that mark a class as a Spring bean / component. */
    private static final Set<String> SPRING_COMPONENT_ANNOTATIONS = Set.of(
            "Component", "Service", "Repository", "Controller", "RestController",
            "Configuration", "Bean", "SpringBootApplication"
    );

    /** Suffixes conventionally used for abstraction types (interfaces/ports). */
    private static final Set<String> ABSTRACTION_SUFFIXES = Set.of(
            "Service", "Repository", "Port", "Gateway", "Client", "Facade",
            "Manager", "Handler", "Processor", "Factory", "Provider", "Adapter"
    );

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(this::isSpringComponent)
                .forEach(cls -> analyzeClass(cls, context, findings));

        return findings;
    }

    private boolean isSpringComponent(ClassModel cls) {
        return SPRING_COMPONENT_ANNOTATIONS.stream().anyMatch(cls::hasAnnotation);
    }

    private void analyzeClass(ClassModel cls, RuleContext context, List<Finding> findings) {
        for (FieldModel field : cls.getFields()) {
            if (!isInjectedField(field)) {
                continue;
            }
            String fieldType = extractSimpleType(field.type());
            if (fieldType == null || fieldType.isBlank()) {
                continue;
            }

            // If the type name ends in a conventional abstraction suffix, skip it
            if (hasAbstractionSuffix(fieldType)) {
                continue;
            }

            // Look up the field's type in the project model
            Optional<ClassModel> fieldTypeCls = resolveType(fieldType, field.type(), context);
            if (fieldTypeCls.isEmpty()) {
                continue;
            }

            ClassModel dependencyClass = fieldTypeCls.get();

            // Violation: the dependency is a concrete class (not interface, not abstract)
            // AND it is a Spring component (so it should have an abstraction extracted)
            boolean isDependencySpringComponent = isSpringComponent(dependencyClass);
            boolean isDependencyConcrete = dependencyClass.getClassType() == ClassType.CLASS
                    && !dependencyClass.isAbstract();

            if (isDependencyConcrete && isDependencySpringComponent) {
                findings.add(Finding.builder()
                        .ruleId("SOLID005")
                        .ruleName("SOLID005")
                        .severity(FindingSeverity.HIGH)
                        .category(RuleCategory.SOLID)
                        .message(String.format(
                                "Class '%s' depends on concrete class '%s' instead of an abstraction. Violates Dependency Inversion Principle.",
                                cls.getSimpleName(), fieldType))
                        .recommendation(String.format(
                                "Introduce an interface/abstract class for '%s' and depend on the abstraction.",
                                fieldType))
                        .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : "")
                        .lineNumber(cls.getBeginLine())
                        .columnNumber(0)
                        .codeSnippet(String.format("@Injected %s %s; // in class %s",
                                field.type(), field.name(), cls.getSimpleName()))
                        .build());
            }
        }
    }

    /**
     * Returns true if the field has any injection annotation.
     */
    private boolean isInjectedField(FieldModel field) {
        if (field.isInjected()) {
            return true;
        }
        return INJECT_ANNOTATIONS.stream().anyMatch(field::hasAnnotation);
    }

    /**
     * Strips generic type parameters to get the raw type name.
     * e.g. {@code List<String>} → {@code List}, {@code UserServiceImpl} → {@code UserServiceImpl}.
     */
    private String extractSimpleType(String type) {
        if (type == null) {
            return null;
        }
        int genericStart = type.indexOf('<');
        String raw = genericStart >= 0 ? type.substring(0, genericStart) : type;
        int lastDot = raw.lastIndexOf('.');
        return lastDot >= 0 ? raw.substring(lastDot + 1) : raw;
    }

    /**
     * Attempts to resolve the field type against the project model, trying both
     * simple name and the fully-qualified type string.
     */
    private Optional<ClassModel> resolveType(String simpleName, String rawType, RuleContext context) {
        // Try exact FQN match first
        Optional<ClassModel> result = context.project().findClass(rawType);
        if (result.isPresent()) {
            return result;
        }
        // Try simple name search via streaming (no direct simple-name lookup in API)
        return context.project().allClasses()
                .filter(c -> c.getSimpleName().equals(simpleName))
                .findFirst();
    }

    private boolean hasAbstractionSuffix(String typeName) {
        return ABSTRACTION_SUFFIXES.stream().anyMatch(typeName::endsWith);
    }
}
