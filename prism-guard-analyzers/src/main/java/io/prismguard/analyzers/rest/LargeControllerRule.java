package io.prismguard.analyzers.rest;

import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.annotation.Check;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.Set;
import java.util.ArrayList;
import java.util.List;

/**
 * REST002 — Large REST Controller.
 * A @RestController with too many endpoint methods typically handles multiple
 * resources or concerns and should be split into focused controllers per resource.
 */
@Analyzer(type = "large-controller")
public class LargeControllerRule {

    private static final Set<String> HTTP_MAPPING_ANNOTATIONS = Set.of(
            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping",
            "RequestMapping"
    );

    @Check
    public List<Finding> check(RuleContext context) {
        int maxEndpoints = context.parameter("maxEndpoints", 15);

        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.hasAnnotation("RestController") || cls.hasAnnotation("Controller"))
                .forEach(cls -> {
                    long endpointCount = cls.getMethods().stream()
                            .filter(m -> m.getAnnotations().stream()
                                    .anyMatch(a -> HTTP_MAPPING_ANNOTATIONS.stream()
                                            .anyMatch(ma -> a.equals(ma) || a.endsWith("." + ma))))
                            .count();

                    if (endpointCount > maxEndpoints) {
                        findings.add(Finding.builder()
                                .ruleId("REST002")
                                .ruleName("REST002")
                                .severity(FindingSeverity.MEDIUM)
                                .category(RuleCategory.REST_API)
                                .message(String.format(
                                        "Controller '%s' exposes %d endpoints (max: %d). A large controller likely handles multiple resources and violates the Single Responsibility Principle.",
                                        cls.getSimpleName(), endpointCount, maxEndpoints))
                                .recommendation("Split the controller into smaller, resource-focused controllers — one controller per domain resource (e.g., OrderController, ShipmentController). This improves discoverability, reduces merge conflicts, and aligns with REST resource modelling.")
                                .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                                .lineNumber(cls.getBeginLine())
                                .columnNumber(0)
                                .codeSnippet("@RestController class " + cls.getSimpleName() + " { /* " + endpointCount + " endpoints */ }")
                                .build());
                    }
                });

        return findings;
    }
}
