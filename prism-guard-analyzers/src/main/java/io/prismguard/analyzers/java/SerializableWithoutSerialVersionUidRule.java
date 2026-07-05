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
 * JAVA014 — Serializable Class Without serialVersionUID.
 * Classes implementing Serializable without declaring a serialVersionUID risk
 * InvalidClassException after any refactoring that changes the JVM-computed UID.
 */
@Analyzer(type = "serializable-without-serial-version-uid")
public class SerializableWithoutSerialVersionUidRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.getClassType() == ClassType.CLASS)
                .filter(cls -> isSerializable(cls.getImplementedInterfaces()))
                .filter(cls -> !hasSerialVersionUID(cls))
                .forEach(cls -> {
                    findings.add(Finding.builder()
                            .ruleId("JAVA014")
                            .ruleName("JAVA014")
                            .severity(FindingSeverity.MEDIUM)
                            .category(RuleCategory.JAVA_QUALITY)
                            .message(String.format(
                                    "Class '%s' implements Serializable but does not declare a 'serialVersionUID' field.",
                                    cls.getSimpleName()))
                            .recommendation("Add 'private static final long serialVersionUID = 1L;' to make serialization behaviour explicit and stable across refactorings. Without it the JVM auto-computes a UID that changes whenever the class structure changes, causing deserialization failures.")
                            .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : cls.getFullyQualifiedName())
                            .lineNumber(cls.getBeginLine())
                            .columnNumber(0)
                            .codeSnippet("class " + cls.getSimpleName() + " implements Serializable { /* no serialVersionUID */ }")
                            .build());
                });

        return findings;
    }

    private boolean isSerializable(List<String> interfaces) {
        return interfaces.stream().anyMatch(i ->
                i.equals("Serializable") || i.equals("java.io.Serializable"));
    }

    private boolean hasSerialVersionUID(io.prismguard.api.model.ClassModel cls) {
        return cls.getFields().stream().anyMatch(f -> "serialVersionUID".equals(f.name()));
    }
}
