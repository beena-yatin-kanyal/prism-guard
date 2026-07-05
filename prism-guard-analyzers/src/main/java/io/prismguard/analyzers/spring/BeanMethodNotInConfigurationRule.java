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
 * SPRING016 — @Bean Method Not in @Configuration Class.
 * @Bean methods placed in classes not annotated with @Configuration are processed
 * in "lite" mode: inter-bean method calls are NOT proxied, breaking singleton
 * guarantees and potentially causing multiple bean instances to be created.
 */
@Analyzer(type = "bean-method-not-in-configuration")
public class BeanMethodNotInConfigurationRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> !cls.hasAnnotation("Configuration"))
                .filter(cls -> cls.isSpringComponent())
                .forEach(cls -> {
                    cls.getMethods().forEach(method -> {
                        if (method.hasAnnotation("Bean")) {
                            findings.add(Finding.builder()
                                    .ruleId("SPRING016")
                                    .ruleName("SPRING016")
                                    .severity(FindingSeverity.HIGH)
                                    .category(RuleCategory.SPRING_BOOT)
                                    .message(String.format(
                                            "Method '%s.%s()' is annotated with @Bean but its class is not annotated with @Configuration. It runs in 'lite' mode — inter-bean method calls are not proxied and singleton guarantees may be violated.",
                                            cls.getSimpleName(), method.getName()))
                                    .recommendation("Move @Bean factory methods to a class annotated with @Configuration. In full @Configuration mode, Spring proxies the class so that inter-bean @Bean method calls always return the same singleton instance.")
                                    .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                                    .lineNumber(method.getBeginLine())
                                    .columnNumber(0)
                                    .codeSnippet("@Bean " + method.getName() + "() in non-@Configuration class " + cls.getSimpleName())
                                    .build());
                        }
                    });
                });

        return findings;
    }
}
