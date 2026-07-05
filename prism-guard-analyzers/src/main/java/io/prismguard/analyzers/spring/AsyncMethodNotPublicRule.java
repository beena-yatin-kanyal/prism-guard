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
 * SPRING013 — @Async on Non-Public Method.
 * Spring's proxy-based AOP cannot intercept non-public methods.
 * @Async placed on a private or package-private method is silently ignored
 * — the method runs synchronously in the caller's thread.
 */
@Analyzer(type = "async-method-not-public")
public class AsyncMethodNotPublicRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.isSpringComponent())
                .forEach(cls -> {
                    cls.getMethods().forEach(method -> {
                        if (method.hasAnnotation("Async") && !method.isPublic()) {
                            findings.add(Finding.builder()
                                    .ruleId("SPRING013")
                                    .ruleName("SPRING013")
                                    .severity(FindingSeverity.HIGH)
                                    .category(RuleCategory.SPRING_BOOT)
                                    .message(String.format(
                                            "Method '%s.%s()' is annotated with @Async but is not public. Spring's proxy cannot intercept it — the method will execute synchronously.",
                                            cls.getSimpleName(), method.getName()))
                                    .recommendation("Make the @Async method public and ensure it is called from a different Spring bean (not self-invocation), so that the proxy can intercept the call and delegate to the thread pool.")
                                    .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                                    .lineNumber(method.getBeginLine())
                                    .columnNumber(0)
                                    .codeSnippet("@Async " + (method.isStatic() ? "static " : "") + method.getName() + "()")
                                    .build());
                        }
                    });
                });

        return findings;
    }
}
