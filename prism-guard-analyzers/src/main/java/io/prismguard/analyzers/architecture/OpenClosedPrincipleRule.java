package io.prismguard.analyzers.architecture;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.ClassModel;
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
 * SOLID002 — Open/Closed Principle Violation.
 *
 * Detects chains of {@code instanceof} checks that indicate type-based branching
 * instead of polymorphism. Methods with 3 or more {@code instanceof} expressions
 * across their if/else-if chain are flagged.
 */
@Analyzer(type = "open-closed-principle")
public class OpenClosedPrincipleRule {

    private static final Logger log = LoggerFactory.getLogger(OpenClosedPrincipleRule.class);

    @Check
    public List<Finding> check(RuleContext context) {
        int instanceofThreshold = context.parameter("instanceofThreshold", 3);

        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.getSourceFile() != null)
                .forEach(cls -> analyzeClass(cls, findings, instanceofThreshold));

        return findings;
    }

    private void analyzeClass(ClassModel cls, List<Finding> findings, int instanceofThreshold) {
        Path sourceFile = cls.getSourceFile();
        try {
            CompilationUnit cu = StaticJavaParser.parse(sourceFile);

            cu.findAll(MethodDeclaration.class).forEach(method -> {
                int instanceofCount = countInstanceofChecks(method);
                if (instanceofCount >= instanceofThreshold) {
                    int lineNumber = method.getBegin()
                            .map(pos -> pos.line)
                            .orElse(cls.getBeginLine());

                    findings.add(Finding.builder()
                            .ruleId("SOLID002")
                            .ruleName("SOLID002")
                            .severity(FindingSeverity.MEDIUM)
                            .category(RuleCategory.SOLID)
                            .message(String.format(
                                    "Method '%s.%s()' uses %d instanceof checks. Consider replacing with polymorphism.",
                                    cls.getSimpleName(), method.getNameAsString(), instanceofCount))
                            .recommendation("Use the Strategy or Visitor pattern to eliminate instanceof type-switching.")
                            .filePath(sourceFile.toString())
                            .lineNumber(lineNumber)
                            .columnNumber(0)
                            .codeSnippet(buildSnippet(method))
                            .build());
                }
            });
        } catch (Exception e) {
            log.warn("SOLID002: Failed to parse source file '{}': {}", sourceFile, e.getMessage());
        }
    }

    /**
     * Counts all {@code instanceof} expressions within the method body,
     * including nested if/else-if chains.
     */
    private int countInstanceofChecks(MethodDeclaration method) {
        return method.findAll(InstanceOfExpr.class).size();
    }

    private String buildSnippet(MethodDeclaration method) {
        String signature = method.getDeclarationAsString(false, false, true);
        return signature.length() > 120 ? signature.substring(0, 117) + "..." : signature;
    }

}
