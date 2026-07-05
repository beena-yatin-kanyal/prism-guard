package io.prismguard.analyzers.spring;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import java.nio.file.Path;
import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.ClassModel;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Analyzer(type = "hardcoded-credentials")
public class HardcodedCredentialsRule {

    private static final Logger log = LoggerFactory.getLogger(HardcodedCredentialsRule.class);

    private static final Set<String> CREDENTIAL_KEYWORDS = Set.of(
            "password", "secret", "key", "token", "credential", "apikey", "api_key", "passwd"
    );

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses().forEach(classModel -> {
            Path sourceFile = classModel.getSourceFile();
            if (sourceFile == null || !java.nio.file.Files.exists(sourceFile)) {
                return;
            }

            try {
                CompilationUnit cu = StaticJavaParser.parse(sourceFile);
                String className = classModel.getSimpleName();

                cu.findAll(FieldDeclaration.class).forEach(fieldDecl -> {
                    fieldDecl.getAnnotations().forEach(annotation -> {
                        if (!isValueAnnotation(annotation)) {
                            return;
                        }

                        String annotationValue = extractAnnotationValue(annotation);
                        if (annotationValue == null) {
                            return;
                        }

                        // If the value uses Spring EL or property placeholder, it's safe
                        if (annotationValue.contains("${") || annotationValue.contains("#{")) {
                            return;
                        }

                        // Check if any variable in this field has a credential-like name
                        fieldDecl.getVariables().forEach(variable -> {
                            String fieldName = variable.getNameAsString();
                            if (isCredentialField(fieldName)) {
                                int line = annotation.getBegin().map(p -> p.line).orElse(0);
                                findings.add(Finding.builder()
                                        .ruleId("SPRING010")
                                        .ruleName("SPRING010")
                                        .severity(FindingSeverity.CRITICAL)
                                        .category(RuleCategory.SECURITY)
                                        .message(String.format(
                                                "Field '%s.%s' appears to contain a hardcoded credential "
                                                        + "in @Value annotation",
                                                className, fieldName))
                                        .recommendation("Use Spring's @Value with property placeholders "
                                                + "(${app.secret}) and externalize secrets to environment "
                                                + "variables or a secrets manager")
                                        .filePath(sourceFile.toAbsolutePath().toString())
                                        .lineNumber(line)
                                        .columnNumber(0)
                                        .codeSnippet("@Value(\"" + maskValue(annotationValue) + "\")")
                                        .build());
                            }
                        });
                    });
                });
            } catch (Exception parseException) {
                log.debug("Could not parse file {}: {}",
                        sourceFile.toAbsolutePath().toString(), parseException.getMessage());
            }
        });

        return findings;
    }

    private boolean isValueAnnotation(AnnotationExpr annotation) {
        String name = annotation.getNameAsString();
        return name.equals("Value") || name.endsWith(".Value");
    }

    private String extractAnnotationValue(AnnotationExpr annotation) {
        if (annotation instanceof SingleMemberAnnotationExpr single) {
            if (single.getMemberValue() instanceof StringLiteralExpr str) {
                return str.asString();
            }
            // Handle string literal directly
            return single.getMemberValue().toString().replace("\"", "");
        }
        // For NormalAnnotationExpr, look for the "value" member
        return annotation.findAll(StringLiteralExpr.class).stream()
                .findFirst()
                .map(StringLiteralExpr::asString)
                .orElse(null);
    }

    private boolean isCredentialField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String lower = fieldName.toLowerCase();
        return CREDENTIAL_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private String maskValue(String value) {
        if (value == null || value.length() <= 4) {
            return "***";
        }
        return value.substring(0, 2) + "***";
    }
}
