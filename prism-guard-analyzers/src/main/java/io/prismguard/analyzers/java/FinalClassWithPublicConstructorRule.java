package io.prismguard.analyzers.java;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.ClassType;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.model.MethodModel;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;

@Analyzer(type = "final-class-public-constructor")
public class FinalClassWithPublicConstructorRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(classModel -> classModel.isAbstract())
                .filter(classModel -> classModel.getClassType() == ClassType.CLASS)
                .forEach(classModel -> {
                    List<MethodModel> methods = classModel.getMethods();
                    boolean hasAbstractMethod = methods.stream().anyMatch(MethodModel::isAbstract);

                    if (!hasAbstractMethod) {
                        findings.add(Finding.builder()
                                .ruleId("JAVA009")
                                .ruleName("JAVA009")
                                .severity(FindingSeverity.LOW)
                                .category(RuleCategory.JAVA_QUALITY)
                                .message(String.format(
                                        "Class '%s' is abstract but has no abstract methods. "
                                                + "Remove abstract modifier or add abstract methods.",
                                        classModel.getSimpleName()))
                                .recommendation("Either remove the abstract modifier to make this a concrete class, "
                                        + "or add abstract methods to define a contract for subclasses.")
                                .filePath(classModel.getSourceFile() != null
                                        ? classModel.getSourceFile().toString()
                                        : classModel.getFullyQualifiedName())
                                .lineNumber(classModel.getBeginLine())
                                .columnNumber(0)
                                .codeSnippet("abstract class " + classModel.getSimpleName())
                                .build());
                    }
                });

        return findings;
    }
}
