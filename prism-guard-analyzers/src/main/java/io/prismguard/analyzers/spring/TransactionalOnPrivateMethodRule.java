package io.prismguard.analyzers.spring;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;

@Analyzer(type = "transactional-on-private-method")
public class TransactionalOnPrivateMethodRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses().forEach(classModel -> {
            String className = classModel.getSimpleName();
            classModel.getMethods().forEach(method -> {
                boolean isTransactional = method.hasAnnotation("Transactional")
                        || method.hasAnnotation("org.springframework.transaction.annotation.Transactional")
                        || method.hasAnnotation("javax.transaction.Transactional")
                        || method.hasAnnotation("jakarta.transaction.Transactional");

                if (isTransactional && !method.isPublic()) {
                    findings.add(Finding.builder()
                            .ruleId("SPRING011")
                            .ruleName("SPRING011")
                            .severity(FindingSeverity.HIGH)
                            .category(RuleCategory.TRANSACTION)
                            .message(String.format(
                                    "Method '%s.%s()' is private but annotated @Transactional. "
                                            + "Spring AOP cannot proxy private methods.",
                                    className, method.getName()))
                            .recommendation("Make the method public/protected or move the @Transactional "
                                    + "to a public method that calls it.")
                            .filePath(classModel.getSourceFile() != null
                                    ? classModel.getSourceFile().toString()
                                    : classModel.getFullyQualifiedName())
                            .lineNumber(method.getBeginLine())
                            .columnNumber(0)
                            .codeSnippet("@Transactional private " + method.getReturnType()
                                    + " " + method.getName() + "()")
                            .build());
                }
            });
        });

        return findings;
    }
}
