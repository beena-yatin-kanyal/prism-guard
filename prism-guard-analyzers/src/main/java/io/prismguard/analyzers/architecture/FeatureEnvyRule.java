package io.prismguard.analyzers.architecture;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ARCH005 — Feature Envy.
 *
 * Uses JavaParser to detect methods that call methods on external objects far
 * more than on their own class. Such methods may belong in the class they are
 * most interested in.
 *
 * Thresholds: externalCalls {@literal >} selfCalls * 2  AND  externalCalls {@literal >} 5.
 */
@Analyzer(type = "feature-envy")
public class FeatureEnvyRule {

    private static final Logger log = LoggerFactory.getLogger(FeatureEnvyRule.class);

    @Check
    public List<Finding> check(RuleContext context) {
        int externalCallThreshold = context.parameter("externalCallThreshold", 5);
        int externalToSelfRatio   = context.parameter("externalToSelfRatio", 2);

        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.getSourceFile() != null)
                .forEach(cls -> analyzeClass(cls, findings, externalCallThreshold, externalToSelfRatio));

        return findings;
    }

    private void analyzeClass(ClassModel cls, List<Finding> findings, int externalCallThreshold, int externalToSelfRatio) {
        Path sourceFile = cls.getSourceFile();
        try {
            CompilationUnit cu = StaticJavaParser.parse(sourceFile);

            // Collect field names declared in this class — calls on them are considered "self"
            Set<String> ownFieldNames = collectFieldNames(cu);

            cu.findAll(MethodDeclaration.class).forEach(method -> {
                CallCounts counts = countCalls(method, ownFieldNames);
                if (counts.externalCalls > counts.selfCalls * externalToSelfRatio
                        && counts.externalCalls > externalCallThreshold) {

                    int lineNumber = method.getBegin()
                            .map(pos -> pos.line)
                            .orElse(cls.getBeginLine());

                    findings.add(Finding.builder()
                            .ruleId("ARCH005")
                            .ruleName("ARCH005")
                            .severity(FindingSeverity.MEDIUM)
                            .category(RuleCategory.MAINTAINABILITY)
                            .message(String.format(
                                    "Method '%s.%s()' makes %d calls on external objects vs %d on self. May have feature envy.",
                                    cls.getSimpleName(), method.getNameAsString(),
                                    counts.externalCalls, counts.selfCalls))
                            .recommendation("Consider moving this method to the class it's most interested in.")
                            .filePath(sourceFile.toString())
                            .lineNumber(lineNumber)
                            .columnNumber(0)
                            .codeSnippet(method.getDeclarationAsString(false, false, true))
                            .build());
                }
            });
        } catch (Exception e) {
            log.warn("ARCH005: Failed to parse source file '{}': {}", sourceFile, e.getMessage());
        }
    }

    /**
     * Collects names of all fields declared in the compilation unit (class-level).
     */
    private Set<String> collectFieldNames(CompilationUnit cu) {
        Set<String> names = new HashSet<>();
        cu.findAll(FieldDeclaration.class).forEach(fd ->
                fd.getVariables().stream()
                        .map(VariableDeclarator::getNameAsString)
                        .forEach(names::add));
        return names;
    }

    /**
     * Counts self-calls vs external calls within a method declaration.
     *
     * A call is considered a "self call" if:
     * - It has no explicit scope (implicit this).
     * - Its scope is the literal "this".
     * - Its scope is a known own field name.
     *
     * All other calls (scoped on a local variable, parameter, or another object) are "external".
     */
    private CallCounts countCalls(MethodDeclaration method, Set<String> ownFieldNames) {
        int selfCalls = 0;
        int externalCalls = 0;

        // Also collect parameter names so we can skip counting on "this" params
        Set<String> paramNames = new HashSet<>();
        method.getParameters().forEach(p -> paramNames.add(p.getNameAsString()));

        for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
            Optional<com.github.javaparser.ast.expr.Expression> scopeOpt = call.getScope();

            if (scopeOpt.isEmpty()) {
                // No explicit scope → implicit this
                selfCalls++;
            } else {
                String scopeName = scopeOpt.get().toString();
                if ("this".equals(scopeName) || ownFieldNames.contains(scopeName)) {
                    selfCalls++;
                } else {
                    externalCalls++;
                }
            }
        }

        return new CallCounts(selfCalls, externalCalls);
    }

    private record CallCounts(int selfCalls, int externalCalls) {
    }
}
