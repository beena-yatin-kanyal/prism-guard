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
 * REST001 — REST Endpoint Not Returning ResponseEntity.
 * Endpoints that return raw objects instead of ResponseEntity cannot control
 * HTTP status codes, headers, or conditionally return different response shapes.
 * Using ResponseEntity makes the contract explicit and predictable.
 */
@Analyzer(type = "endpoint-missing-response-entity")
public class EndpointMissingResponseEntityRule {

    private static final Set<String> HTTP_MAPPING_ANNOTATIONS = Set.of(
            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping",
            "RequestMapping"
    );

    private static final Set<String> REACTIVE_TYPES = Set.of("Mono", "Flux");

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.hasAnnotation("RestController"))
                .forEach(cls -> {
                    cls.getMethods().forEach(method -> {
                        boolean isEndpoint = method.getAnnotations().stream()
                                .anyMatch(a -> HTTP_MAPPING_ANNOTATIONS.stream()
                                        .anyMatch(ma -> a.equals(ma) || a.endsWith("." + ma)));

                        if (!isEndpoint) return;

                        String returnType = method.getReturnType();
                        if (returnType == null) return;

                        boolean usesResponseEntity = returnType.startsWith("ResponseEntity")
                                || returnType.contains("ResponseEntity");
                        boolean isVoid = "void".equals(returnType);
                        boolean isReactive = REACTIVE_TYPES.stream()
                                .anyMatch(t -> returnType.startsWith(t + "<ResponseEntity") || returnType.startsWith(t + "<Void"));

                        if (!usesResponseEntity && !isVoid && !isReactive) {
                            findings.add(Finding.builder()
                                    .ruleId("REST001")
                                    .ruleName("REST001")
                                    .severity(FindingSeverity.LOW)
                                    .category(RuleCategory.REST_API)
                                    .message(String.format(
                                            "Endpoint '%s.%s()' returns '%s' instead of ResponseEntity<?>. HTTP status code, headers and conditional responses cannot be controlled.",
                                            cls.getSimpleName(), method.getName(), returnType))
                                    .recommendation("Wrap the return type in ResponseEntity<" + returnType + "> so that HTTP status codes (200, 201, 204, 404 …) and response headers can be set explicitly rather than relying on Spring defaults.")
                                    .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                                    .lineNumber(method.getBeginLine())
                                    .columnNumber(0)
                                    .codeSnippet(cls.getSimpleName() + "." + method.getName() + "() → " + returnType)
                                    .build());
                        }
                    });
                });

        return findings;
    }
}
