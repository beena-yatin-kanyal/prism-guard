package io.prismguard.analyzers.architecture;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.ClassModel;
import io.prismguard.api.model.ClassType;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SOLID004 — Interface Segregation Principle Violation.
 *
 * Detects:
 * 1. Fat interfaces with more than 10 public abstract methods.
 * 2. Classes forced to implement methods they don't use (throw UnsupportedOperationException or have empty bodies).
 */
@Analyzer(type = "interface-segregation")
public class InterfaceSegregationRule {

    private static final Logger log = LoggerFactory.getLogger(InterfaceSegregationRule.class);

    @Check
    public List<Finding> check(RuleContext context) {
        int fatInterfaceThreshold = context.parameter("fatInterfaceThreshold", 10);

        List<Finding> findings = new ArrayList<>();

        // Build a map of interface FQN -> method count for fat-interface detection
        Map<String, Integer> interfaceMethodCounts = context.project().allClasses()
                .filter(cls -> cls.getClassType() == ClassType.INTERFACE)
                .collect(Collectors.toMap(
                        ClassModel::getFullyQualifiedName,
                        cls -> (int) cls.getMethods().stream()
                                .filter(m -> m.isPublic() && m.isAbstract())
                                .count()
                ));

        // Rule part 1: Fat interfaces
        context.project().allClasses()
                .filter(cls -> cls.getClassType() == ClassType.INTERFACE)
                .forEach(iface -> {
                    long methodCount = iface.getMethods().stream()
                            .filter(m -> m.isPublic() && m.isAbstract())
                            .count();
                    if (methodCount > fatInterfaceThreshold) {
                        findings.add(Finding.builder()
                                .ruleId("SOLID004")
                                .ruleName("SOLID004")
                                .severity(FindingSeverity.MEDIUM)
                                .category(RuleCategory.SOLID)
                                .message(String.format(
                                        "Interface '%s' declares %d methods. Consider splitting into smaller, focused interfaces.",
                                        iface.getSimpleName(), methodCount))
                                .recommendation("Apply ISP by splitting '" + iface.getSimpleName() + "' into cohesive, role-specific interfaces.")
                                .filePath(iface.getSourceFile() != null ? iface.getSourceFile().toString() : "")
                                .lineNumber(iface.getBeginLine())
                                .columnNumber(0)
                                .codeSnippet(String.format("interface %s { /* %d methods */ }", iface.getSimpleName(), methodCount))
                                .build());
                    }
                });

        // Rule part 2: Classes forced to implement unused methods
        context.project().allClasses()
                .filter(cls -> cls.getClassType() == ClassType.CLASS)
                .filter(cls -> !cls.getImplementedInterfaces().isEmpty())
                .filter(cls -> cls.getSourceFile() != null)
                .forEach(cls -> analyzeImplementingClass(cls, interfaceMethodCounts, fatInterfaceThreshold, context, findings));

        return findings;
    }

    private void analyzeImplementingClass(
            ClassModel cls,
            Map<String, Integer> interfaceMethodCounts,
            int fatInterfaceThreshold,
            RuleContext context,
            List<Finding> findings) {

        Path sourceFile = cls.getSourceFile();
        try {
            CompilationUnit cu = StaticJavaParser.parse(sourceFile);

            // Count methods in this class that are unsupported or empty
            long unusedMethodCount = cu.findAll(MethodDeclaration.class).stream()
                    .filter(m -> m.getAnnotationByName("Override").isPresent())
                    .filter(m -> m.getBody().isPresent())
                    .filter(m -> isUnusedImplementation(m.getBody().get()))
                    .count();

            if (unusedMethodCount == 0) {
                return;
            }

            // Find which implemented interfaces are fat enough that forced impl is a concern
            for (String iface : cls.getImplementedInterfaces()) {
                String simpleName = extractSimpleName(iface);
                boolean ifaceIsFat = interfaceMethodCounts.entrySet().stream()
                        .anyMatch(e -> (e.getKey().endsWith("." + simpleName) || e.getKey().equals(iface))
                                && e.getValue() > fatInterfaceThreshold / 2);

                if (ifaceIsFat || unusedMethodCount > 1) {
                    findings.add(Finding.builder()
                            .ruleId("SOLID004")
                            .ruleName("SOLID004")
                            .severity(FindingSeverity.MEDIUM)
                            .category(RuleCategory.SOLID)
                            .message(String.format(
                                    "Class '%s' implements '%s' but doesn't use all methods (throws UnsupportedOperationException). Consider interface segregation.",
                                    cls.getSimpleName(), simpleName))
                            .recommendation(String.format(
                                    "Split interface '%s' so that '%s' only implements methods it actually uses.",
                                    simpleName, cls.getSimpleName()))
                            .filePath(sourceFile.toString())
                            .lineNumber(cls.getBeginLine())
                            .columnNumber(0)
                            .codeSnippet(String.format("class %s implements %s { /* %d unused methods */ }",
                                    cls.getSimpleName(), simpleName, unusedMethodCount))
                            .build());
                    break; // Report once per class, not once per interface
                }
            }
        } catch (Exception e) {
            log.warn("SOLID004: Failed to parse source file '{}': {}", sourceFile, e.getMessage());
        }
    }

    private boolean isUnusedImplementation(BlockStmt body) {
        // Check for throw new UnsupportedOperationException(...)
        boolean throwsUnsupported = body.findAll(ThrowStmt.class).stream()
                .anyMatch(throwStmt -> {
                    if (throwStmt.getExpression() instanceof ObjectCreationExpr creation) {
                        String typeName = creation.getTypeAsString();
                        return typeName.equals("UnsupportedOperationException")
                                || typeName.equals("java.lang.UnsupportedOperationException");
                    }
                    return false;
                });
        if (throwsUnsupported) {
            return true;
        }
        // Check for empty body
        return body.getStatements().isEmpty();
    }

    private String extractSimpleName(String fullyQualifiedOrSimpleName) {
        int lastDot = fullyQualifiedOrSimpleName.lastIndexOf('.');
        return lastDot >= 0
                ? fullyQualifiedOrSimpleName.substring(lastDot + 1)
                : fullyQualifiedOrSimpleName;
    }
}
