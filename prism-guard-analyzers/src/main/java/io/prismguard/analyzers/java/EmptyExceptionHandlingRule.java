package io.prismguard.analyzers.java;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.CatchClause;
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

@Analyzer(type = "empty-exception-handling")
public class EmptyExceptionHandlingRule {

    private static final Logger log = LoggerFactory.getLogger(EmptyExceptionHandlingRule.class);

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

                cu.findAll(CatchClause.class).forEach(catchClause -> {
                    boolean isEmpty = catchClause.getBody().getStatements().isEmpty();
                    if (isEmpty) {
                        String className = cu.getPrimaryTypeName().orElse("Unknown");
                        String methodName = catchClause.findAncestor(
                                com.github.javaparser.ast.body.MethodDeclaration.class)
                                .map(m -> m.getNameAsString())
                                .orElse("unknown");
                        int line = catchClause.getBegin()
                                .map(p -> p.line)
                                .orElse(0);

                        findings.add(Finding.builder()
                                .ruleId("JAVA005")
                                .ruleName("JAVA005")
                                .severity(FindingSeverity.HIGH)
                                .category(RuleCategory.JAVA_QUALITY)
                                .message(String.format(
                                        "Catch block in '%s.%s()' at line %d swallows exception silently",
                                        className, methodName, line))
                                .recommendation("Log the exception, rethrow it, or handle it meaningfully")
                                .filePath(sourceFile.toAbsolutePath().toString())
                                .lineNumber(line)
                                .columnNumber(0)
                                .codeSnippet("catch (" + catchClause.getParameter().getTypeAsString() + " " +
                                        catchClause.getParameter().getNameAsString() + ") {}")
                                .build());
                    }
                });
            } catch (Exception parseException) {
                log.debug("Could not parse file {}: {}", sourceFile.toAbsolutePath().toString(), parseException.getMessage());
            }
        });

        return findings;
    }
}
