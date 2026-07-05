package io.prismguard.analyzers.spring;

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
 * SPRING015 — REST Endpoint Returns Void.
 * Endpoint methods returning void give the framework no information about the
 * HTTP status code to use, defaulting to 200 even on delete or async operations.
 * Returning ResponseEntity gives full control over status, headers, and body.
 */
@Analyzer(type = "rest-endpoint-returns-void")
public class RestEndpointReturnsVoidRule {

    private static final Set<String> HTTP_MAPPING_ANNOTATIONS = Set.of(
            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping",
            "RequestMapping"
    );

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.hasAnnotation("RestController") || cls.hasAnnotation("Controller"))
                .forEach(cls -> {
                    cls.getMethods().forEach(method -> {
                        boolean isEndpoint = method.getAnnotations().stream()
                                .anyMatch(a -> HTTP_MAPPING_ANNOTATIONS.stream()
                                        .anyMatch(ma -> a.equals(ma) || a.endsWith("." + ma)));
                        boolean returnsVoid = "void".equals(method.getReturnType());
                        if (isEndpoint && returnsVoid) {
                            findings.add(Finding.builder()
                                    .ruleId("SPRING015")
                                    .ruleName("SPRING015")
                                    .severity(FindingSeverity.MEDIUM)
                                    .category(RuleCategory.REST_API)
                                    .message(String.format(
                                            "Endpoint method '%s.%s()' returns void. The HTTP status code cannot be controlled and defaults to 200.",
                                            cls.getSimpleName(), method.getName()))
                                    .recommendation("Return ResponseEntity<Void> (or ResponseEntity<?>) to explicitly set the HTTP status code (e.g., 204 No Content for DELETE, 202 Accepted for async operations).")
                                    .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                                    .lineNumber(method.getBeginLine())
                                    .columnNumber(0)
                                    .codeSnippet(cls.getSimpleName() + "." + method.getName() + "() → void")
                                    .build());
                        }
                    });
                });

        return findings;
    }
}
