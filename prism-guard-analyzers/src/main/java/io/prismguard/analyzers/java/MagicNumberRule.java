package io.prismguard.analyzers.java;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import java.nio.file.Path;
import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Analyzer(type = "magic-number")
public class MagicNumberRule {

    private static final Logger log = LoggerFactory.getLogger(MagicNumberRule.class);

    // Allowed numeric literals that are not considered magic numbers
    private static final Set<String> ALLOWED_VALUES = Set.of("0", "1", "-1", "2", "100");

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allFiles().forEach(javaFileModel -> {
            Path sourceFile = javaFileModel.getFilePath();

            if (sourceFile == null || !java.nio.file.Files.exists(sourceFile)) {
                return;
            }

            try {
                CompilationUnit cu = StaticJavaParser.parse(sourceFile);
                String primaryClassName = cu.getPrimaryTypeName().orElse("Unknown");

                cu.findAll(MethodDeclaration.class).forEach(method -> {
                    String methodName = method.getNameAsString();

                    // Check integer literals in method bodies
                    method.getBody().ifPresent(body -> {
                        body.findAll(IntegerLiteralExpr.class).forEach(literal -> {
                            String value = literal.getValue();
                            if (!isAllowedValue(value) && !isNegatedAllowedValue(literal)) {
                                int line = literal.getBegin().map(p -> p.line).orElse(0);
                                findings.add(buildFinding(primaryClassName, methodName, value,
                                        line, sourceFile.toAbsolutePath().toString()));
                            }
                        });

                        body.findAll(LongLiteralExpr.class).forEach(literal -> {
                            String rawValue = literal.getValue();
                            // Strip trailing L/l from long literals for allowed-value check
                            String value = rawValue.replaceAll("[Ll]$", "");
                            if (!isAllowedValue(value)) {
                                int line = literal.getBegin().map(p -> p.line).orElse(0);
                                findings.add(buildFinding(primaryClassName, methodName, rawValue,
                                        line, sourceFile.toAbsolutePath().toString()));
                            }
                        });

                        body.findAll(DoubleLiteralExpr.class).forEach(literal -> {
                            String value = literal.getValue();
                            if (!isAllowedValue(value)) {
                                int line = literal.getBegin().map(p -> p.line).orElse(0);
                                findings.add(buildFinding(primaryClassName, methodName, value,
                                        line, sourceFile.toAbsolutePath().toString()));
                            }
                        });
                    });
                });
            } catch (Exception parseException) {
                log.debug("Could not parse file {}: {}", sourceFile.toAbsolutePath().toString(), parseException.getMessage());
            }
        });

        return findings;
    }

    private boolean isAllowedValue(String value) {
        return ALLOWED_VALUES.contains(value);
    }

    private boolean isNegatedAllowedValue(IntegerLiteralExpr literal) {
        // Handle -1 which is represented as UnaryExpr(MINUS, 1) in some parsers
        return literal.getParentNode()
                .filter(parent -> parent instanceof UnaryExpr)
                .map(parent -> (UnaryExpr) parent)
                .filter(unary -> unary.getOperator() == UnaryExpr.Operator.MINUS)
                .map(unary -> ALLOWED_VALUES.contains("-" + literal.getValue()))
                .orElse(false);
    }

    private Finding buildFinding(String className, String methodName, String value,
                                  int line, String filePath) {
        return Finding.builder()
                .ruleId("JAVA010")
                .ruleName("JAVA010")
                .severity(FindingSeverity.INFO)
                .category(RuleCategory.JAVA_QUALITY)
                .message(String.format(
                        "Magic number %s found in '%s.%s()' at line %d",
                        value, className, methodName, line))
                .recommendation("Replace magic numbers with named constants (static final fields)")
                .filePath(filePath)
                .lineNumber(line)
                .columnNumber(0)
                .codeSnippet(value)
                .build();
    }
}
