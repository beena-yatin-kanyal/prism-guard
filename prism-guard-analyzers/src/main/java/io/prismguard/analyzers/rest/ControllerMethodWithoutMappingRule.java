package io.prismguard.analyzers.rest;

import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.annotation.Check;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * REST003 — Public Controller Method Without HTTP Mapping.
 * A public method in a @RestController without any HTTP mapping annotation is
 * not an endpoint, but it pollutes the controller's public API and may confuse
 * developers who expect all public methods to be endpoints.
 * Such methods belong in a service or helper class.
 */
@Analyzer(type = "controller-method-without-mapping")
public class ControllerMethodWithoutMappingRule {

    private static final Set<String> HTTP_MAPPING_ANNOTATIONS = Set.of(
            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping",
            "RequestMapping"
    );

    private static final Set<String> LIFECYCLE_ANNOTATIONS = Set.of(
            "PostConstruct", "PreDestroy", "EventListener", "ExceptionHandler",
            "ModelAttribute", "InitBinder", "Override"
    );

    private static final Set<String> EXCLUDED_METHOD_NAMES = Set.of(
            "toString", "hashCode", "equals", "clone"
    );

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.hasAnnotation("RestController") || cls.hasAnnotation("Controller"))
                .forEach(cls -> {
                    cls.getMethods().forEach(method -> {
                        if (!method.isPublic()) return;
                        if (EXCLUDED_METHOD_NAMES.contains(method.getName())) return;

                        boolean hasHttpMapping = method.getAnnotations().stream()
                                .anyMatch(a -> HTTP_MAPPING_ANNOTATIONS.stream()
                                        .anyMatch(ma -> a.equals(ma) || a.endsWith("." + ma)));

                        boolean hasLifecycleAnnotation = method.getAnnotations().stream()
                                .anyMatch(a -> LIFECYCLE_ANNOTATIONS.stream()
                                        .anyMatch(la -> a.equals(la) || a.endsWith("." + la)));

                        if (!hasHttpMapping && !hasLifecycleAnnotation) {
                            findings.add(Finding.builder()
                                    .ruleId("REST003")
                                    .ruleName("REST003")
                                    .severity(FindingSeverity.LOW)
                                    .category(RuleCategory.REST_API)
                                    .message(String.format(
                                            "Public method '%s.%s()' in a @RestController has no HTTP mapping annotation. It is not an endpoint but exposes business logic in the presentation layer.",
                                            cls.getSimpleName(), method.getName()))
                                    .recommendation("Move non-endpoint public methods to a @Service class. Controllers should contain only endpoint handlers and exception handlers (@ExceptionHandler).")
                                    .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                                    .lineNumber(method.getBeginLine())
                                    .columnNumber(0)
                                    .codeSnippet("public " + method.getReturnType() + " " + method.getName() + "() /* in @RestController, no mapping */")
                                    .build());
                        }
                    });
                });

        return findings;
    }
}
