package io.prismguard.analyzers.java;

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

@Analyzer(type = "utility-class-design")
public class UtilityClassDesignRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(classModel -> !classModel.isAbstract())
                .filter(this::isUtilityClass)
                .forEach(classModel -> {
                    boolean hasPrivateNoArgConstructor = classModel.getMethods().stream()
                            .filter(m -> m.getName().equals("<init>") || m.getName().equals(classModel.getSimpleName()))
                            .anyMatch(m -> !m.isPublic() && m.getParameterTypes().isEmpty());

                    if (!hasPrivateNoArgConstructor) {
                        findings.add(Finding.builder()
                                .ruleId("JAVA006")
                                .ruleName("JAVA006")
                                .severity(FindingSeverity.LOW)
                                .category(RuleCategory.JAVA_QUALITY)
                                .message(String.format(
                                        "Utility class '%s' should have a private no-arg constructor "
                                                + "to prevent instantiation",
                                        classModel.getSimpleName()))
                                .recommendation("Add: private " + classModel.getSimpleName()
                                        + "() { throw new UnsupportedOperationException(\"Utility class\"); }")
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

    private boolean isUtilityClass(ClassModel classModel) {
        List<MethodModel> methods = classModel.getMethods();
        if (methods.isEmpty()) {
            return false;
        }
        // Exclude constructors (by name matching simpleName or <init>) when checking if all are static
        List<MethodModel> nonConstructorMethods = methods.stream()
                .filter(m -> !m.getName().equals("<init>")
                        && !m.getName().equals(classModel.getSimpleName()))
                .toList();

        if (nonConstructorMethods.isEmpty()) {
            return false;
        }

        return nonConstructorMethods.stream().allMatch(MethodModel::isStatic);
    }
}
