package io.prismguard.analyzers.java;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;

@Analyzer(type = "god-class")
public class GodClassRule {

    @Check
    public List<Finding> check(RuleContext context) {
        int maxPublicMethods = context.parameter("maxPublicMethods", 20);
        int maxLines         = context.parameter("maxLines", 500);
        int maxInstanceFields = context.parameter("maxInstanceFields", 15);

        List<Finding> findings = new ArrayList<>();

        context.project().allClasses().forEach(classModel -> {
            long publicMethodCount = classModel.getMethods().stream()
                    .filter(m -> !m.isStatic() && m.isPublic())
                    .count();
            int lineCount = classModel.lineCount();
            long instanceFieldCount = classModel.getFields().stream()
                    .filter(f -> !f.isStatic())
                    .count();

            boolean isGodClass = publicMethodCount > maxPublicMethods
                    || lineCount > maxLines
                    || instanceFieldCount > maxInstanceFields;

            if (isGodClass) {
                findings.add(Finding.builder()
                        .ruleId("JAVA001")
                        .ruleName("JAVA001")
                        .severity(FindingSeverity.HIGH)
                        .category(RuleCategory.JAVA_QUALITY)
                        .message(String.format(
                                "Class '%s' is a God Class (%d methods, %d lines, %d fields)",
                                classModel.getSimpleName(),
                                publicMethodCount,
                                lineCount,
                                instanceFieldCount))
                        .recommendation("Break this class into smaller, focused classes following SRP")
                        .filePath(classModel.getSourceFile() != null
                                ? classModel.getSourceFile().toString()
                                : classModel.getFullyQualifiedName())
                        .lineNumber(classModel.getBeginLine())
                        .columnNumber(0)
                        .codeSnippet(classModel.getSimpleName())
                        .build());
            }
        });

        return findings;
    }
}
