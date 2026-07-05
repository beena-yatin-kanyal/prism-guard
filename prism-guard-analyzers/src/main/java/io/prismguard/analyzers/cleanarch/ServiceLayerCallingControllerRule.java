package io.prismguard.analyzers.cleanarch;

import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.annotation.Check;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.JavaFileModel;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * CLEAN004 — Service Layer Importing Presentation Layer.
 * Services and repositories must never import from controller, web, or REST
 * packages. Such imports invert the dependency direction, tangle the service
 * layer with HTTP concerns, and make the application impossible to reuse
 * outside a web context.
 */
@Analyzer(type = "service-layer-calling-controller")
public class ServiceLayerCallingControllerRule {

    private static final Set<String> SERVICE_PACKAGES = Set.of(".service.", ".application.", ".usecase.");
    private static final Set<String> FORBIDDEN_IMPORT_SEGMENTS = Set.of(
            ".controller.", ".web.", ".rest.", ".resource."
    );

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allFiles()
                .filter(file -> isServicePackage(file.getPackageName()))
                .forEach(file -> {
                    file.getImports().forEach(imp -> {
                        FORBIDDEN_IMPORT_SEGMENTS.stream()
                                .filter(imp::contains)
                                .findFirst()
                                .ifPresent(segment -> {
                                    String primaryClass = file.getClasses().isEmpty()
                                            ? file.getPackageName()
                                            : file.getClasses().get(0).getSimpleName();
                                    findings.add(Finding.builder()
                                            .ruleId("CLEAN004")
                                            .ruleName("CLEAN004")
                                            .severity(FindingSeverity.HIGH)
                                            .category(RuleCategory.CLEAN_ARCHITECTURE)
                                            .message(String.format(
                                                    "Service class '%s' imports presentation-layer class '%s'. Dependencies must only flow inward — services must not know about controllers.",
                                                    primaryClass, imp))
                                            .recommendation("Remove the import and invert the dependency. If the service needs to return web-specific types, introduce a shared DTO in a neutral package, or use an output port interface that the controller implements.")
                                            .filePath(file.getFilePath() != null ? file.getFilePath().toString() : "")
                                            .lineNumber(0)
                                            .columnNumber(0)
                                            .codeSnippet("import " + imp + ";")
                                            .build());
                                });
                    });
                });

        return findings;
    }

    private boolean isServicePackage(String packageName) {
        if (packageName == null) return false;
        return SERVICE_PACKAGES.stream().anyMatch(packageName::contains);
    }
}
