package io.prismguard.analyzers.architecture;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.model.JavaFileModel;
import io.prismguard.api.rule.RuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ARCH001 — Layering Violation.
 *
 * Enforces layered architecture constraints by detecting illegal imports and
 * annotations across architectural layers (PRESENTATION, APPLICATION, DOMAIN,
 * INFRASTRUCTURE), classified by package naming conventions.
 */
@Analyzer(type = "layering-violation")
public class LayeringViolationRule {

    private static final Logger log = LoggerFactory.getLogger(LayeringViolationRule.class);

    private enum Layer {
        PRESENTATION, APPLICATION, DOMAIN, INFRASTRUCTURE, UNKNOWN
    }

    /** Annotations that domain classes must not carry (they belong to outer layers). */
    private static final Set<String> FORBIDDEN_DOMAIN_ANNOTATIONS = Set.of(
            "Controller", "RestController", "Repository",
            "org.springframework.stereotype.Controller",
            "org.springframework.web.bind.annotation.RestController",
            "org.springframework.stereotype.Repository"
    );

    /** Import segments that are forbidden in DOMAIN layer source files. */
    private static final Set<String> DOMAIN_FORBIDDEN_IMPORT_SEGMENTS = Set.of(
            ".controller.", ".web.", ".rest."
    );

    /** Import segments that are forbidden in INFRASTRUCTURE layer source files. */
    private static final Set<String> INFRA_FORBIDDEN_IMPORT_SEGMENTS = Set.of(
            ".controller.", ".web.", ".rest."
    );

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allFiles().forEach(file -> analyzeFile(file, context, findings));

        return findings;
    }

    private void analyzeFile(JavaFileModel file, RuleContext context, List<Finding> findings) {
        String packageName = file.getPackageName();
        Layer layer = classifyLayer(packageName);

        if (layer == Layer.UNKNOWN) {
            return;
        }

        List<String> imports = file.getImports();

        switch (layer) {
            case DOMAIN -> {
                // Domain must not import from presentation layer
                for (String importedClass : imports) {
                    for (String forbidden : DOMAIN_FORBIDDEN_IMPORT_SEGMENTS) {
                        if (importedClass.contains(forbidden)) {
                            addImportViolation(file, layer, importedClass, Layer.PRESENTATION, findings);
                            break;
                        }
                    }
                }
                // Domain classes must not carry Spring stereotype annotations
                file.getClasses().forEach(cls -> {
                    for (String annotation : FORBIDDEN_DOMAIN_ANNOTATIONS) {
                        if (cls.hasAnnotation(annotation)) {
                            findings.add(Finding.builder()
                                    .ruleId("ARCH001")
                                    .ruleName("ARCH001")
                                    .severity(FindingSeverity.HIGH)
                                    .category(RuleCategory.ARCHITECTURE)
                                    .message(String.format(
                                            "Class '%s' in DOMAIN layer carries @%s annotation. Domain classes should not depend on Spring infrastructure.",
                                            cls.getSimpleName(), annotation.contains(".") ? annotation.substring(annotation.lastIndexOf('.') + 1) : annotation))
                                    .recommendation("Remove Spring stereotype annotations from domain classes. Use application or infrastructure layer for Spring beans.")
                                    .filePath(file.getFilePath() != null ? file.getFilePath().toString() : "")
                                    .lineNumber(cls.getBeginLine())
                                    .columnNumber(0)
                                    .codeSnippet(String.format("@%s class %s {}", annotation, cls.getSimpleName()))
                                    .build());
                            break;
                        }
                    }
                });
            }
            case INFRASTRUCTURE -> {
                for (String importedClass : imports) {
                    for (String forbidden : INFRA_FORBIDDEN_IMPORT_SEGMENTS) {
                        if (importedClass.contains(forbidden)) {
                            addImportViolation(file, layer, importedClass, Layer.PRESENTATION, findings);
                            break;
                        }
                    }
                }
            }
            default -> {
                // PRESENTATION and APPLICATION layers have no upward restrictions in this model
            }
        }
    }

    private void addImportViolation(
            JavaFileModel file,
            Layer sourceLayer,
            String importedClass,
            Layer targetLayer,
            List<Finding> findings) {

        String primaryClassName = file.getClasses().isEmpty()
                ? file.getPackageName()
                : file.getClasses().get(0).getSimpleName();

        findings.add(Finding.builder()
                .ruleId("ARCH001")
                .ruleName("ARCH001")
                .severity(FindingSeverity.HIGH)
                .category(RuleCategory.ARCHITECTURE)
                .message(String.format(
                        "Class '%s' in %s layer imports '%s' from %s layer. This violates layering constraints.",
                        primaryClassName, sourceLayer.name(), importedClass, targetLayer.name()))
                .recommendation(String.format(
                        "Remove the dependency from '%s' to the %s layer. Introduce an abstraction (interface/event) if communication is required.",
                        primaryClassName, targetLayer.name().toLowerCase()))
                .filePath(file.getFilePath() != null ? file.getFilePath().toString() : "")
                .lineNumber(0)
                .columnNumber(0)
                .codeSnippet(String.format("import %s;", importedClass))
                .build());
    }

    private Layer classifyLayer(String packageName) {
        if (packageName == null) {
            return Layer.UNKNOWN;
        }
        if (packageName.contains(".controller") || packageName.contains(".web")
                || packageName.contains(".rest")) {
            return Layer.PRESENTATION;
        }
        if (packageName.contains(".service") || packageName.contains(".application")) {
            return Layer.APPLICATION;
        }
        if (packageName.contains(".domain") || packageName.contains(".model")
                || packageName.contains(".entity")) {
            return Layer.DOMAIN;
        }
        if (packageName.contains(".repository") || packageName.contains(".persistence")
                || packageName.contains(".infrastructure")) {
            return Layer.INFRASTRUCTURE;
        }
        return Layer.UNKNOWN;
    }
}
