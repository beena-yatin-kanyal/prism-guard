package io.prismguard.analyzers.architecture;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.ClassModel;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.model.MethodModel;
import io.prismguard.api.rule.RuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ARCH007 — Utility Class in Domain Layer.
 *
 * Detects utility classes (all-static methods, name ends in Util/Utils/Helper/Helpers)
 * that are located in domain packages (package contains {@code .domain.} or {@code .model.}).
 * Utility classes have no place in the domain layer and should live in a dedicated utility package.
 */
@Analyzer(type = "util-class-in-domain")
public class UtilClassInDomainRule {

    private static final Logger log = LoggerFactory.getLogger(UtilClassInDomainRule.class);

    private static final Set<String> UTIL_SUFFIXES = Set.of(
            "Util", "Utils", "Helper", "Helpers"
    );

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> isDomainPackage(cls.getPackageName()))
                .filter(cls -> hasUtilSuffix(cls.getSimpleName()))
                .filter(cls -> isUtilityClass(cls))
                .forEach(cls -> findings.add(buildFinding(cls)));

        return findings;
    }

    private boolean isDomainPackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        return packageName.contains(".domain") || packageName.contains(".model");
    }

    private boolean hasUtilSuffix(String simpleName) {
        return UTIL_SUFFIXES.stream().anyMatch(simpleName::endsWith);
    }

    /**
     * A class is considered a "utility class" if all its non-constructor public methods are static.
     * We require at least one method to avoid false positives on marker/empty classes.
     */
    private boolean isUtilityClass(ClassModel cls) {
        List<MethodModel> methods = cls.getMethods();
        if (methods.isEmpty()) {
            return false;
        }
        // All public methods must be static
        long publicMethodCount = methods.stream()
                .filter(MethodModel::isPublic)
                .count();
        if (publicMethodCount == 0) {
            return false;
        }
        long nonStaticPublicCount = methods.stream()
                .filter(MethodModel::isPublic)
                .filter(m -> !m.isStatic())
                .count();
        return nonStaticPublicCount == 0;
    }

    private Finding buildFinding(ClassModel cls) {
        return Finding.builder()
                .ruleId("ARCH007")
                .ruleName("ARCH007")
                .severity(FindingSeverity.LOW)
                .category(RuleCategory.CLEAN_ARCHITECTURE)
                .message(String.format(
                        "Utility class '%s' is located in domain package '%s'. Move it to a utility package.",
                        cls.getSimpleName(), cls.getPackageName()))
                .recommendation("Keep domain packages clean. Utility classes belong in a separate utility package.")
                .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : "")
                .lineNumber(cls.getBeginLine())
                .columnNumber(0)
                .codeSnippet(String.format("class %s { /* utility class in domain package %s */ }",
                        cls.getSimpleName(), cls.getPackageName()))
                .build();
    }
}
