package io.prismguard.analyzers.java;

import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.annotation.Check;
import io.prismguard.api.model.ClassType;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;

/**
 * JAVA012 — Abstract Class With No Abstract Methods.
 * An abstract class that declares no abstract methods cannot enforce a contract.
 * Such classes should either be made concrete or converted to an interface.
 */
@Analyzer(type = "abstract-class-no-abstract-methods")
public class AbstractClassNoAbstractMethodsRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.isAbstract() && cls.getClassType() == ClassType.CLASS)
                .forEach(cls -> {
                    boolean hasAbstractMethod = cls.getMethods().stream().anyMatch(m -> m.isAbstract());
                    if (!hasAbstractMethod) {
                        findings.add(Finding.builder()
                                .ruleId("JAVA012")
                                .ruleName("JAVA012")
                                .severity(FindingSeverity.LOW)
                                .category(RuleCategory.JAVA_QUALITY)
                                .message(String.format(
                                        "Abstract class '%s' declares no abstract methods. It cannot enforce a contract on subclasses.",
                                        cls.getSimpleName()))
                                .recommendation("If subclasses need a common contract, declare abstract methods. If shared state/behaviour is the only goal and no contract is needed, consider making the class concrete with a protected constructor, or refactor to an interface with default methods.")
                                .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                                .lineNumber(cls.getBeginLine())
                                .columnNumber(0)
                                .codeSnippet("abstract class " + cls.getSimpleName() + " { /* no abstract methods */ }")
                                .build());
                    }
                });

        return findings;
    }
}
