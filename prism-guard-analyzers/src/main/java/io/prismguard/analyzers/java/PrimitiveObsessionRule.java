package io.prismguard.analyzers.java;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.model.MethodModel;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Analyzer(type = "primitive-obsession")
public class PrimitiveObsessionRule {

    private static final Set<String> PRIMITIVE_TYPES = Set.of(
            "int", "long", "boolean", "double", "float", "char", "byte", "short", "String",
            "Integer", "Long", "Boolean", "Double", "Float", "Character", "Byte", "Short"
    );
    @Check
    public List<Finding> check(RuleContext context) {
        int minTotalParams    = context.parameter("minTotalParams", 4);
        int minPrimitiveParams = context.parameter("minPrimitiveParams", 3);

        List<Finding> findings = new ArrayList<>();

        context.project().allClasses().forEach(classModel -> {
            String className = classModel.getSimpleName();
            classModel.getMethods().forEach(method -> {
                List<String> params = method.getParameterTypes();
                if (params == null) {
                    return;
                }
                int totalParams = params.size();
                if (totalParams <= minTotalParams) {
                    return;
                }
                long primitiveCount = params.stream()
                        .filter(this::isPrimitiveOrString)
                        .count();

                if (primitiveCount >= minPrimitiveParams) {
                    String methodLabel = isConstructor(method, className)
                            ? className + " (constructor)"
                            : className + "." + method.getName() + "()";

                    findings.add(Finding.builder()
                            .ruleId("JAVA008")
                            .ruleName("JAVA008")
                            .severity(FindingSeverity.LOW)
                            .category(RuleCategory.JAVA_QUALITY)
                            .message(String.format(
                                    "Method/constructor '%s' has %d primitive parameters. "
                                            + "Consider encapsulating related params in a value object.",
                                    methodLabel, primitiveCount))
                            .recommendation("Introduce a value object or parameter object to group "
                                    + "related primitive parameters.")
                            .filePath(classModel.getSourceFile() != null
                                    ? classModel.getSourceFile().toString()
                                    : classModel.getFullyQualifiedName())
                            .lineNumber(method.getBeginLine())
                            .columnNumber(0)
                            .codeSnippet(methodLabel)
                            .build());
                }
            });
        });

        return findings;
    }

    private boolean isPrimitiveOrString(String type) {
        if (type == null) {
            return false;
        }
        // Strip array markers and generics for base type check
        String baseType = type.replaceAll("\\[.*]", "").replaceAll("<.*>", "").trim();
        return PRIMITIVE_TYPES.contains(baseType);
    }

    private boolean isConstructor(MethodModel method, String className) {
        return method.getName().equals("<init>") || method.getName().equals(className);
    }
}
