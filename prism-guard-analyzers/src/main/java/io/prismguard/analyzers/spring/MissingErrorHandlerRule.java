package io.prismguard.analyzers.spring;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;

@Analyzer(type = "missing-error-handler")
public class MissingErrorHandlerRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        boolean hasControllerAdvice = context.project().allClasses()
                .anyMatch(classModel ->
                        classModel.hasAnnotation("ControllerAdvice")
                                || classModel.hasAnnotation("RestControllerAdvice")
                                || classModel.hasAnnotation("org.springframework.web.bind.annotation.ControllerAdvice")
                                || classModel.hasAnnotation("org.springframework.web.bind.annotation.RestControllerAdvice"));

        boolean hasRestControllers = context.project().allClasses()
                .anyMatch(classModel ->
                        classModel.hasAnnotation("RestController")
                                || classModel.hasAnnotation("Controller")
                                || classModel.hasAnnotation("org.springframework.web.bind.annotation.RestController")
                                || classModel.hasAnnotation("org.springframework.stereotype.Controller"));

        if (!hasControllerAdvice && hasRestControllers) {
            String projectRoot = context.configuration().getProjectRoot() != null
                    ? context.configuration().getProjectRoot().toString()
                    : "";

            findings.add(Finding.builder()
                    .ruleId("SPRING009")
                    .ruleName("SPRING009")
                    .severity(FindingSeverity.MEDIUM)
                    .category(RuleCategory.REST_API)
                    .message("No @ControllerAdvice found. REST APIs should have centralized exception handling.")
                    .recommendation("Create a @RestControllerAdvice class to handle exceptions globally "
                            + "and return consistent error responses.")
                    .filePath(projectRoot)
                    .lineNumber(0)
                    .columnNumber(0)
                    .codeSnippet("@RestControllerAdvice // missing")
                    .build());
        }

        return findings;
    }
}
