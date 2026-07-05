package io.prismguard.analyzers.spring;

import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.annotation.Check;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;

/**
 * SPRING017 — HttpSession Usage in REST Controller.
 * REST APIs should be stateless. Injecting or using HttpSession in a
 * @RestController couples the service to sticky sessions, breaks horizontal
 * scaling, and is incompatible with JWT / token-based authentication.
 */
@Analyzer(type = "http-session-in-rest-controller")
public class HttpSessionInRestControllerRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.hasAnnotation("RestController"))
                .forEach(cls -> {
                    // Check for HttpSession in constructor-injected or field-injected
                    boolean sessionInField = cls.getFields().stream()
                            .anyMatch(f -> isHttpSession(f.type()));

                    if (sessionInField) {
                        findings.add(Finding.builder()
                                .ruleId("SPRING017")
                                .ruleName("SPRING017")
                                .severity(FindingSeverity.HIGH)
                                .category(RuleCategory.SPRING_BOOT)
                                .message(String.format(
                                        "REST controller '%s' holds an HttpSession field. REST APIs must be stateless — server-side session state prevents horizontal scaling and is incompatible with token-based auth.",
                                        cls.getSimpleName()))
                                .recommendation("Remove HttpSession from the controller. Use stateless authentication (JWT, OAuth2 tokens) and store user state client-side or in a distributed cache (Redis). If session support is mandatory, document the sticky-session requirement explicitly.")
                                .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                                .lineNumber(cls.getBeginLine())
                                .columnNumber(0)
                                .codeSnippet("@RestController class " + cls.getSimpleName() + " { HttpSession session; }")
                                .build());
                    }

                    // Also check for HttpSession appearing as a method parameter type
                    cls.getMethods().forEach(method -> {
                        boolean sessionInParams = method.getParameterTypes().stream()
                                .anyMatch(HttpSessionInRestControllerRule::isHttpSession);
                        if (sessionInParams) {
                            findings.add(Finding.builder()
                                    .ruleId("SPRING017")
                                    .ruleName("SPRING017")
                                    .severity(FindingSeverity.HIGH)
                                    .category(RuleCategory.SPRING_BOOT)
                                    .message(String.format(
                                            "REST controller method '%s.%s()' takes HttpSession as a parameter. REST APIs must be stateless.",
                                            cls.getSimpleName(), method.getName()))
                                    .recommendation("Remove HttpSession parameters from REST controller methods. Use stateless authentication tokens and pass user identity via JWT claims or security context instead.")
                                    .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                                    .lineNumber(method.getBeginLine())
                                    .columnNumber(0)
                                    .codeSnippet(cls.getSimpleName() + "." + method.getName() + "(HttpSession session)")
                                    .build());
                        }
                    });
                });

        return findings;
    }

    private static boolean isHttpSession(String type) {
        return "HttpSession".equals(type)
                || "javax.servlet.http.HttpSession".equals(type)
                || "jakarta.servlet.http.HttpSession".equals(type);
    }
}
