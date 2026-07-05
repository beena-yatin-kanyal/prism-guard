package io.prismguard.analyzers.spring;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;

@Analyzer(type = "repository-in-service")
public class RepositoryInServiceRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(classModel ->
                        classModel.hasAnnotation("Controller")
                                || classModel.hasAnnotation("RestController")
                                || classModel.hasAnnotation("org.springframework.stereotype.Controller")
                                || classModel.hasAnnotation("org.springframework.web.bind.annotation.RestController"))
                .forEach(classModel -> {
                    String className = classModel.getSimpleName();
                    classModel.getFields().forEach(field -> {
                        String fieldType = field.type();
                        if (fieldType != null && fieldType.endsWith("Repository")) {
                            findings.add(Finding.builder()
                                    .ruleId("SPRING004")
                                    .ruleName("SPRING004")
                                    .severity(FindingSeverity.HIGH)
                                    .category(RuleCategory.ARCHITECTURE)
                                    .message(String.format(
                                            "Controller '%s' directly injects repository '%s'. "
                                                    + "Access data only through service layer.",
                                            className, fieldType))
                                    .recommendation("Controllers should not access repositories directly. "
                                            + "Introduce a service layer.")
                                    .filePath(classModel.getSourceFile() != null
                                            ? classModel.getSourceFile().toString()
                                            : classModel.getFullyQualifiedName())
                                    .lineNumber(classModel.getBeginLine())
                                    .columnNumber(0)
                                    .codeSnippet(fieldType + " " + field.name())
                                    .build());
                        }
                    });
                });

        return findings;
    }
}
