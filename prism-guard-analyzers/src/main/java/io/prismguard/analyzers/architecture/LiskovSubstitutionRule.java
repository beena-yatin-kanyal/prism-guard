package io.prismguard.analyzers.architecture;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
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

/**
 * SOLID003 — Liskov Substitution Principle Violation.
 *
 * Detects subclasses that break the LSP contract by:
 * 1. Overriding a parent method and throwing {@link UnsupportedOperationException}.
 * 2. Overriding a parent method with an empty body.
 */
@Analyzer(type = "liskov-substitution")
public class LiskovSubstitutionRule {

    private static final Logger log = LoggerFactory.getLogger(LiskovSubstitutionRule.class);

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.getClassType() == ClassType.CLASS)
                .filter(cls -> hasMeaningfulSuperClass(cls))
                .filter(cls -> cls.getSourceFile() != null)
                .forEach(cls -> analyzeClass(cls, findings));

        return findings;
    }

    private boolean hasMeaningfulSuperClass(ClassModel cls) {
        String superClass = cls.getSuperClass();
        return superClass != null
                && !superClass.isBlank()
                && !superClass.equals("Object")
                && !superClass.equals("java.lang.Object");
    }

    private void analyzeClass(ClassModel cls, List<Finding> findings) {
        Path sourceFile = cls.getSourceFile();
        try {
            CompilationUnit cu = StaticJavaParser.parse(sourceFile);

            cu.findAll(MethodDeclaration.class).forEach(method -> {
                // Only examine @Override methods — these are the overriding ones
                boolean isOverride = method.getAnnotationByName("Override").isPresent();
                if (!isOverride) {
                    return;
                }

                String violation = detectViolation(method);
                if (violation != null) {
                    int lineNumber = method.getBegin()
                            .map(pos -> pos.line)
                            .orElse(cls.getBeginLine());

                    findings.add(Finding.builder()
                            .ruleId("SOLID003")
                            .ruleName("SOLID003")
                            .severity(FindingSeverity.MEDIUM)
                            .category(RuleCategory.SOLID)
                            .message(String.format(
                                    "Method '%s.%s()' may violate LSP: overrides parent method but %s.",
                                    cls.getSimpleName(), method.getNameAsString(), violation))
                            .recommendation(String.format(
                                    "Subclasses should be substitutable for their supertype. If '%s' can't implement '%s', reconsider the inheritance hierarchy.",
                                    cls.getSimpleName(), method.getNameAsString()))
                            .filePath(sourceFile.toString())
                            .lineNumber(lineNumber)
                            .columnNumber(0)
                            .codeSnippet(method.getDeclarationAsString(false, false, true))
                            .build());
                }
            });
        } catch (Exception e) {
            log.warn("SOLID003: Failed to parse source file '{}': {}", sourceFile, e.getMessage());
        }
    }

    /**
     * Returns a human-readable description of the violation, or {@code null} if no violation is found.
     */
    private String detectViolation(MethodDeclaration method) {
        if (!method.getBody().isPresent()) {
            return null;
        }
        BlockStmt body = method.getBody().get();

        // Check 1: throws UnsupportedOperationException
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
            return "throws UnsupportedOperationException";
        }

        // Check 2: empty body (no statements, or only comments)
        boolean hasNoStatements = body.getStatements().isEmpty();
        if (hasNoStatements) {
            return "has empty body";
        }

        return null;
    }
}
