package io.prismguard.analyzers.java;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.ClassType;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;

@Analyzer(type = "public-field")
public class PublicFieldRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(classModel -> {
                    ClassType classType = classModel.getClassType();
                    return classType != ClassType.RECORD && classType != ClassType.ENUM;
                })
                .forEach(classModel -> {
                    String className = classModel.getSimpleName();
                    classModel.getFields().forEach(field -> {
                        if (!field.isPrivate() && !field.isStatic() && !field.isFinal()) {
                            findings.add(Finding.builder()
                                    .ruleId("JAVA004")
                                    .ruleName("JAVA004")
                                    .severity(FindingSeverity.MEDIUM)
                                    .category(RuleCategory.JAVA_QUALITY)
                                    .message(String.format(
                                            "Field '%s.%s' is public. Encapsulate with private + getter/setter.",
                                            className, field.name()))
                                    .recommendation("Make fields private and provide accessors "
                                            + "to preserve encapsulation")
                                    .filePath(classModel.getSourceFile() != null
                                            ? classModel.getSourceFile().toString()
                                            : classModel.getFullyQualifiedName())
                                    .lineNumber(classModel.getBeginLine())
                                    .columnNumber(0)
                                    .codeSnippet(field.type() + " " + field.name())
                                    .build());
                        }
                    });
                });

        return findings;
    }
}
