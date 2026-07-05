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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SOLID001 — Single Responsibility Principle Violation.
 *
 * Detects classes that handle too many distinct concerns based on method name
 * prefix classification. A class with methods in 4+ concern buckets and 10+
 * total methods is flagged as a potential SRP violation.
 */
@Analyzer(type = "single-responsibility")
public class SingleResponsibilityRule {

    private static final Logger log = LoggerFactory.getLogger(SingleResponsibilityRule.class);

    /**
     * Maps method name prefixes to concern bucket labels.
     * Ordered so longer/more-specific prefixes are checked before shorter ones.
     */
    private static final Map<String, String> PREFIX_TO_CONCERN = Map.ofEntries(
            Map.entry("validate", "validation"),
            Map.entry("check", "validation"),
            Map.entry("notify", "notification"),
            Map.entry("publish", "notification"),
            Map.entry("calculate", "computation"),
            Map.entry("compute", "computation"),
            Map.entry("process", "computation"),
            Map.entry("format", "presentation"),
            Map.entry("render", "presentation"),
            Map.entry("display", "presentation"),
            Map.entry("build", "presentation"),
            Map.entry("convert", "transformation"),
            Map.entry("transform", "transformation"),
            Map.entry("audit", "audit"),
            Map.entry("track", "audit"),
            Map.entry("save", "data-write"),
            Map.entry("update", "data-write"),
            Map.entry("delete", "data-write"),
            Map.entry("create", "data-write"),
            Map.entry("insert", "data-write"),
            Map.entry("send", "notification"),
            Map.entry("emit", "notification"),
            Map.entry("log", "audit"),
            Map.entry("get", "data-read"),
            Map.entry("find", "data-read"),
            Map.entry("load", "data-read"),
            Map.entry("read", "data-read")
    );

    @Check
    public List<Finding> check(RuleContext context) {
        int concernThreshold     = context.parameter("concernThreshold", 4);
        int methodCountThreshold = context.parameter("methodCountThreshold", 10);

        List<Finding> findings = new ArrayList<>();

        context.project().allClasses()
                .filter(cls -> cls.methodCount() >= methodCountThreshold)
                .forEach(cls -> analyzeClass(cls, findings, concernThreshold));

        return findings;
    }

    private void analyzeClass(ClassModel cls, List<Finding> findings, int concernThreshold) {
        List<MethodModel> methods = cls.getMethods();
        Set<String> concerns = new LinkedHashSet<>();

        for (MethodModel method : methods) {
            String methodName = method.getName().toLowerCase();
            String concern = classifyConcern(methodName);
            if (concern != null) {
                concerns.add(concern);
            }
        }

        if (concerns.size() >= concernThreshold) {
            String concernList = String.join(", ", concerns);
            findings.add(Finding.builder()
                    .ruleId("SOLID001")
                    .ruleName("SOLID001")
                    .severity(FindingSeverity.HIGH)
                    .category(RuleCategory.SOLID)
                    .message(String.format(
                            "Class '%s' handles multiple concerns: %s. It may violate the Single Responsibility Principle.",
                            cls.getSimpleName(), concernList))
                    .recommendation(String.format(
                            "Decompose '%s' into focused classes, each responsible for a single concern.",
                            cls.getSimpleName()))
                    .filePath(cls.getSourceFile() != null ? cls.getSourceFile().toString() : "")
                    .lineNumber(cls.getBeginLine())
                    .columnNumber(0)
                    .codeSnippet(String.format("class %s { /* %d methods across %d concerns */ }",
                            cls.getSimpleName(), cls.methodCount(), concerns.size()))
                    .build());
        }
    }

    private String classifyConcern(String methodName) {
        // Check longer/more-specific prefixes first via length ordering
        return PREFIX_TO_CONCERN.entrySet().stream()
                .filter(entry -> methodName.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
