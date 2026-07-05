package io.prismguard.analyzers.spring;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.ClassModel;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.model.MethodModel;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Analyzer(type = "missing-transactional")
public class MissingTransactionalRule {

    private static final Set<String> DB_OPERATION_PREFIXES = Set.of(
            "save", "update", "delete", "create", "insert", "remove", "persist", "merge"
    );

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(classModel -> classModel.hasAnnotation("Service")
                        || classModel.hasAnnotation("org.springframework.stereotype.Service"))
                .forEach(classModel -> {
                    boolean classHasTransactional = classModel.hasAnnotation("Transactional")
                            || classModel.hasAnnotation("org.springframework.transaction.annotation.Transactional")
                            || classModel.hasAnnotation("javax.transaction.Transactional")
                            || classModel.hasAnnotation("jakarta.transaction.Transactional");

                    String className = classModel.getSimpleName();
                    classModel.getMethods().forEach(method -> {
                        if (!method.isPublic()) {
                            return;
                        }
                        if (!looksLikeDatabaseOperation(method.getName())) {
                            return;
                        }
                        if (classHasTransactional) {
                            return;
                        }
                        boolean methodHasTransactional = method.hasAnnotation("Transactional")
                                || method.hasAnnotation("org.springframework.transaction.annotation.Transactional")
                                || method.hasAnnotation("javax.transaction.Transactional")
                                || method.hasAnnotation("jakarta.transaction.Transactional");

                        if (!methodHasTransactional) {
                            findings.add(Finding.builder()
                                    .ruleId("SPRING002")
                                    .ruleName("SPRING002")
                                    .severity(FindingSeverity.MEDIUM)
                                    .category(RuleCategory.TRANSACTION)
                                    .message(String.format(
                                            "Method '%s.%s()' appears to perform database operations "
                                                    + "but lacks @Transactional",
                                            className, method.getName()))
                                    .recommendation("Add @Transactional to ensure atomicity. "
                                            + "Use readOnly=true for read-only operations.")
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

    private boolean looksLikeDatabaseOperation(String methodName) {
        if (methodName == null) {
            return false;
        }
        String lower = methodName.toLowerCase();
        return DB_OPERATION_PREFIXES.stream().anyMatch(lower::startsWith);
    }
}
