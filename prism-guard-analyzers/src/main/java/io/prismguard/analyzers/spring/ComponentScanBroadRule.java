package io.prismguard.analyzers.spring;

import java.nio.file.Path;
import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.ClassModel;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;

@Analyzer(type = "component-scan-broad")
public class ComponentScanBroadRule {

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(classModel ->
                        classModel.hasAnnotation("SpringBootApplication")
                                || classModel.hasAnnotation("ComponentScan")
                                || classModel.hasAnnotation("org.springframework.boot.autoconfigure.SpringBootApplication")
                                || classModel.hasAnnotation("org.springframework.context.annotation.ComponentScan"))
                .forEach(classModel -> {
                    // Check if this class is not at the root package by inspecting the package name depth
                    // An overly broad scan is one where @ComponentScan has no basePackages specified
                    // We detect this heuristically: the class has the annotation but we can't check
                    // annotation attributes via ClassModel API, so we flag any @ComponentScan that
                    // is not accompanied by an explicit package (done via source file analysis)
                    boolean hasComponentScanWithoutPackages = hasUnscopedComponentScan(classModel);

                    if (hasComponentScanWithoutPackages) {
                        findings.add(Finding.builder()
                                .ruleId("SPRING005")
                                .ruleName("SPRING005")
                                .severity(FindingSeverity.MEDIUM)
                                .category(RuleCategory.SPRING_BOOT)
                                .message(String.format(
                                        "Class '%s' uses @ComponentScan without specifying basePackages. "
                                                + "This can slow startup and cause unexpected bean loading.",
                                        classModel.getSimpleName()))
                                .recommendation("Specify basePackages or basePackageClasses "
                                        + "to limit component scanning scope.")
                                .filePath(classModel.getSourceFile() != null
                                        ? classModel.getSourceFile().toString()
                                        : classModel.getFullyQualifiedName())
                                .lineNumber(classModel.getBeginLine())
                                .columnNumber(0)
                                .codeSnippet("@ComponentScan // no basePackages")
                                .build());
                    }
                });

        return findings;
    }

    /**
     * Checks if a class has @ComponentScan (or @SpringBootApplication which includes it)
     * without specifying a base package via the source file content.
     *
     * We parse the source file to look for the annotation text and check if basePackages or
     * basePackageClasses attributes are present.
     */
    private boolean hasUnscopedComponentScan(ClassModel classModel) {
        Path sourceFile = classModel.getSourceFile();
        if (sourceFile == null || !java.nio.file.Files.exists(sourceFile)) {
            // If we can't read the source, conservatively flag @ComponentScan classes
            // that are not at a single-segment package (root package)
            return classModel.hasAnnotation("ComponentScan")
                    && classModel.getPackageName() != null
                    && classModel.getPackageName().contains(".");
        }

        try {
            String content = java.nio.file.Files.readString(sourceFile);
            // Look for @ComponentScan without basePackages or basePackageClasses attributes
            boolean hasComponentScan = content.contains("@ComponentScan");
            if (!hasComponentScan) {
                return false;
            }
            boolean hasBasePackages = content.contains("basePackages")
                    || content.contains("basePackageClasses");
            return !hasBasePackages;
        } catch (java.io.IOException e) {
            return false;
        }
    }
}
