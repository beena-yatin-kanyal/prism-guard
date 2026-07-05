package io.prismguard.analyzers.architecture;

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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ARCH003 — God Package.
 *
 * Finds packages that contain more than 30 classes, indicating a lack of
 * sub-organisation by feature or responsibility. Test packages are excluded.
 */
@Analyzer(type = "god-package")
public class AntiPatternGodPackageRule {

    private static final Logger log = LoggerFactory.getLogger(AntiPatternGodPackageRule.class);

    @Check
    public List<Finding> check(RuleContext context) {
        int godPackageThreshold = context.parameter("godPackageThreshold", 30);

        List<Finding> findings = new ArrayList<>();

        String projectRoot = context.configuration().getProjectRoot() != null
                ? context.configuration().getProjectRoot().toString()
                : "";

        // Group classes by their package, excluding test packages
        Map<String, Long> packageClassCounts = context.project().allClasses()
                .filter(cls -> !isTestPackage(cls.getPackageName()))
                .collect(Collectors.groupingBy(ClassModel::getPackageName, Collectors.counting()));

        packageClassCounts.forEach((packageName, count) -> {
            if (count > godPackageThreshold) {
                findings.add(Finding.builder()
                        .ruleId("ARCH003")
                        .ruleName("ARCH003")
                        .severity(FindingSeverity.MEDIUM)
                        .category(RuleCategory.ARCHITECTURE)
                        .message(String.format(
                                "Package '%s' contains %d classes. Consider organizing into sub-packages by feature/responsibility.",
                                packageName, count))
                        .recommendation("Break large packages into feature-based or responsibility-based sub-packages.")
                        .filePath(projectRoot)
                        .lineNumber(0)
                        .columnNumber(0)
                        .codeSnippet(String.format("package %s; // %d classes", packageName, count))
                        .build());
            }
        });

        return findings;
    }

    private boolean isTestPackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        return packageName.contains(".test.") || packageName.endsWith(".test")
                || packageName.contains(".tests.") || packageName.endsWith(".tests")
                || packageName.contains("test") && (packageName.contains(".test") || packageName.startsWith("test."));
    }
}
