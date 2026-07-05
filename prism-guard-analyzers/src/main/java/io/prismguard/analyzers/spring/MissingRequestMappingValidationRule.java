package io.prismguard.analyzers.spring;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
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

@Analyzer(type = "missing-request-mapping-validation")
public class MissingRequestMappingValidationRule {

    private static final Logger log = LoggerFactory.getLogger(MissingRequestMappingValidationRule.class);

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(classModel ->
                        classModel.hasAnnotation("RestController")
                                || classModel.hasAnnotation("Controller")
                                || classModel.hasAnnotation("org.springframework.web.bind.annotation.RestController")
                                || classModel.hasAnnotation("org.springframework.stereotype.Controller"))
                .forEach(classModel -> {
                    Path sourceFile = classModel.getSourceFile();
                    if (sourceFile == null || !java.nio.file.Files.exists(sourceFile)) {
                        return;
                    }
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(sourceFile);
                        String className = classModel.getSimpleName();

                        cu.findAll(MethodDeclaration.class).forEach(method -> {
                            method.getParameters().forEach(param -> {
                                boolean hasRequestBody = param.getAnnotations().stream()
                                        .anyMatch(ann -> {
                                            String name = ann.getNameAsString();
                                            return name.equals("RequestBody")
                                                    || name.endsWith(".RequestBody");
                                        });

                                if (!hasRequestBody) {
                                    return;
                                }

                                boolean hasValidation = param.getAnnotations().stream()
                                        .anyMatch(ann -> {
                                            String name = ann.getNameAsString();
                                            return name.equals("Valid")
                                                    || name.equals("Validated")
                                                    || name.endsWith(".Valid")
                                                    || name.endsWith(".Validated");
                                        });

                                if (!hasValidation) {
                                    int line = method.getBegin().map(p -> p.line).orElse(0);
                                    findings.add(Finding.builder()
                                            .ruleId("SPRING006")
                                            .ruleName("SPRING006")
                                            .severity(FindingSeverity.MEDIUM)
                                            .category(RuleCategory.REST_API)
                                            .message(String.format(
                                                    "Controller method '%s.%s()' accepts @RequestBody "
                                                            + "without @Valid/@Validated",
                                                    className, method.getNameAsString()))
                                            .recommendation("Add @Valid on @RequestBody parameters and annotate "
                                                    + "your DTO with Bean Validation constraints")
                                            .filePath(sourceFile.toAbsolutePath().toString())
                                            .lineNumber(line)
                                            .columnNumber(0)
                                            .codeSnippet(className + "." + method.getNameAsString() + "()")
                                            .build());
                                }
                            });
                        });
                    } catch (Exception parseException) {
                        log.debug("Could not parse file {}: {}",
                                sourceFile.toAbsolutePath().toString(), parseException.getMessage());
                    }
                });

        return findings;
    }
}
