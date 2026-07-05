package io.prismguard.analyzers.java;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.FieldModel;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Analyzer(type = "mutable-static-field")
public class MutableStaticFieldRule {

    private static final Set<String> LOGGER_NAMES = Set.of("log", "logger", "LOGGER", "LOG");

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses().forEach(classModel -> {
            String className = classModel.getSimpleName();
            classModel.getFields().forEach(field -> {
                if (!field.isStatic()) {
                    return;
                }
                if (isLoggerField(field)) {
                    return;
                }
                // A constant is static + final + UPPER_CASE name
                boolean isConstant = field.isFinal() && isUpperCaseName(field.name());
                if (isConstant) {
                    return;
                }
                // Mutable: static + not final, OR static + final but camelCase name
                boolean isMutable = !field.isFinal()
                        || (field.isFinal() && isCamelCaseName(field.name()));

                if (isMutable) {
                    findings.add(Finding.builder()
                            .ruleId("JAVA007")
                            .ruleName("JAVA007")
                            .severity(FindingSeverity.MEDIUM)
                            .category(RuleCategory.JAVA_QUALITY)
                            .message(String.format(
                                    "Static field '%s.%s' is mutable. "
                                            + "Consider making it final or using thread-safe types.",
                                    className, field.name()))
                            .recommendation("Static mutable state is a thread-safety hazard. "
                                    + "Use final, AtomicXxx, or avoid static state.")
                            .filePath(classModel.getSourceFile() != null
                                    ? classModel.getSourceFile().toString()
                                    : classModel.getFullyQualifiedName())
                            .lineNumber(classModel.getBeginLine())
                            .columnNumber(0)
                            .codeSnippet("static " + field.type() + " " + field.name())
                            .build());
                }
            });
        });

        return findings;
    }

    private boolean isLoggerField(FieldModel field) {
        String type = field.type();
        String name = field.name();
        return (type != null && type.contains("Logger"))
                || LOGGER_NAMES.contains(name);
    }

    private boolean isUpperCaseName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return name.equals(name.toUpperCase()) && name.contains("_") || name.matches("[A-Z][A-Z0-9_]*");
    }

    private boolean isCamelCaseName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return Character.isLowerCase(name.charAt(0));
    }
}
