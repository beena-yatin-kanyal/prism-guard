package io.prismguard.analyzers.java;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;

@Analyzer(type = "high-cyclomatic-complexity")
public class HighCyclomaticComplexityRule {

    @Check
    public List<Finding> check(RuleContext context) {
        int maxComplexity = context.parameter("maxComplexity", 10);

        List<Finding> findings = new ArrayList<>();

        context.project().allClasses().forEach(classModel -> {
            String className = classModel.getSimpleName();
            classModel.getMethods().forEach(method -> {
                int complexity = method.getCyclomaticComplexity();
                if (complexity > maxComplexity) {
                    findings.add(Finding.builder()
                            .ruleId("JAVA003")
                            .ruleName("JAVA003")
                            .severity(FindingSeverity.HIGH)
                            .category(RuleCategory.JAVA_QUALITY)
                            .message(String.format(
                                    "Method '%s.%s()' has cyclomatic complexity of %d (max: %d)",
                                    className, method.getName(), complexity, maxComplexity))
                            .recommendation("Reduce complexity by extracting helper methods, "
                                    + "using polymorphism, or simplifying logic")
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
