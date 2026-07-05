package io.prismguard.analyzers.spring;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;

@Analyzer(type = "controller-business-logic")
public class ControllerBusinessLogicRule {

    @Check
    public List<Finding> check(RuleContext context) {
        int maxMethodLines = context.parameter("maxMethodLines", 30);
        int maxComplexity  = context.parameter("maxComplexity", 5);

        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(classModel ->
                        classModel.hasAnnotation("Controller")
                                || classModel.hasAnnotation("RestController")
                                || classModel.hasAnnotation("org.springframework.stereotype.Controller")
                                || classModel.hasAnnotation("org.springframework.web.bind.annotation.RestController"))
                .forEach(classModel -> {
                    String className = classModel.getSimpleName();
                    classModel.getMethods().forEach(method -> {
                        int lines = method.lineCount();
                        int cc = method.getCyclomaticComplexity();

                        if (lines > maxMethodLines || cc > maxComplexity) {
                            findings.add(Finding.builder()
                                    .ruleId("SPRING003")
                                    .ruleName("SPRING003")
                                    .severity(FindingSeverity.HIGH)
                                    .category(RuleCategory.ARCHITECTURE)
                                    .message(String.format(
                                            "Controller method '%s.%s()' contains complex logic "
                                                    + "(%d lines, complexity %d). "
                                                    + "Move business logic to a @Service.",
                                            className, method.getName(), lines, cc))
                                    .recommendation("Controllers should only handle HTTP concerns. "
                                            + "Delegate all logic to service layer.")
                                    .filePath(classModel.getSourceFile() != null
                                            ? classModel.getSourceFile().toString()
                                            : classModel.getFullyQualifiedName())
                                    .lineNumber(method.getBeginLine())
                                    .columnNumber(0)
                                    .codeSnippet(className + "." + method.getName() + "()")
                                    .build());
                        }
                    });
                });

        return findings;
    }
}
