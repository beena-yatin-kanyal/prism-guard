package io.prismguard.analyzers.spring;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;

@Analyzer(type = "deprecated-spring-pattern")
public class DeprecatedSpringPatternRule {

    private record DeprecatedPattern(String className, String pattern, String alternative) {}

    private static final List<DeprecatedPattern> DEPRECATED_SUPERCLASSES = List.of(
            new DeprecatedPattern(
                    "WebMvcConfigurerAdapter",
                    "extends WebMvcConfigurerAdapter",
                    "implement WebMvcConfigurer interface directly"),
            new DeprecatedPattern(
                    "AsyncConfigurerSupport",
                    "extends AsyncConfigurerSupport",
                    "implement AsyncConfigurer interface directly")
    );

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses().forEach(classModel -> {
            String className = classModel.getSimpleName();
            String superClass = classModel.getSuperClass();

            // Check deprecated superclasses
            for (DeprecatedPattern pattern : DEPRECATED_SUPERCLASSES) {
                if (superClass != null && (superClass.equals(pattern.className())
                        || superClass.endsWith("." + pattern.className()))) {
                    findings.add(Finding.builder()
                            .ruleId("SPRING007")
                            .ruleName("SPRING007")
                            .severity(FindingSeverity.MEDIUM)
                            .category(RuleCategory.SPRING_BOOT)
                            .message(String.format(
                                    "Class '%s' uses deprecated Spring pattern: %s. Use: %s",
                                    className, pattern.pattern(), pattern.alternative()))
                            .recommendation("Migrate to the modern equivalent: " + pattern.alternative())
                            .filePath(classModel.getSourceFile() != null
                                    ? classModel.getSourceFile().toString()
                                    : classModel.getFullyQualifiedName())
                            .lineNumber(classModel.getBeginLine())
                            .columnNumber(0)
                            .codeSnippet("class " + className + " extends " + superClass)
                            .build());
                }
            }

            // Check @EnableWebMvc annotation
            if (classModel.hasAnnotation("EnableWebMvc")
                    || classModel.hasAnnotation("org.springframework.web.servlet.config.annotation.EnableWebMvc")) {
                findings.add(Finding.builder()
                        .ruleId("SPRING007")
                        .ruleName("SPRING007")
                        .severity(FindingSeverity.MEDIUM)
                        .category(RuleCategory.SPRING_BOOT)
                        .message(String.format(
                                "Class '%s' uses deprecated Spring pattern: @EnableWebMvc. "
                                        + "Use: Remove @EnableWebMvc in Spring Boot auto-configured apps",
                                className))
                        .recommendation("In Spring Boot, @EnableWebMvc disables Spring Boot's auto-configuration. "
                                + "Remove it unless you intentionally need full manual MVC control.")
                        .filePath(classModel.getSourceFile() != null
                                ? classModel.getSourceFile().toString()
                                : classModel.getFullyQualifiedName())
                        .lineNumber(classModel.getBeginLine())
                        .columnNumber(0)
                        .codeSnippet("@EnableWebMvc")
                        .build());
            }
        });

        return findings;
    }
}
