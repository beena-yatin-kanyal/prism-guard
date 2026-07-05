package io.prismguard.analyzers.spring;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Analyzer(type = "spring-boot-actuator-missing")
public class SpringBootActuatorMissingRule {

    private static final Logger log = LoggerFactory.getLogger(SpringBootActuatorMissingRule.class);

    @Check
    public List<Finding> check(RuleContext context) {
        List<Finding> findings = new ArrayList<>();

        boolean hasSpringBootApplication = context.project().allClasses()
                .anyMatch(classModel ->
                        classModel.hasAnnotation("SpringBootApplication")
                                || classModel.hasAnnotation("org.springframework.boot.autoconfigure.SpringBootApplication"));

        if (!hasSpringBootApplication) {
            return findings;
        }

        java.nio.file.Path projectRoot = context.configuration().getProjectRoot();
        if (projectRoot == null) {
            return findings;
        }

        File pomFile = projectRoot.resolve("pom.xml").toFile();
        if (!pomFile.exists()) {
            // Try build.gradle as fallback
            File gradleFile = projectRoot.resolve("build.gradle").toFile();
            if (!gradleFile.exists()) {
                return findings;
            }
            // Check gradle file for actuator
            if (!fileContainsActuator(gradleFile)) {
                findings.add(buildFinding(gradleFile.getAbsolutePath()));
            }
            return findings;
        }

        if (!fileContainsActuator(pomFile)) {
            findings.add(buildFinding(pomFile.getAbsolutePath()));
        }

        return findings;
    }

    private boolean fileContainsActuator(File buildFile) {
        try {
            String content = Files.readString(buildFile.toPath());
            return content.contains("spring-boot-starter-actuator")
                    || content.contains("spring-boot-actuator");
        } catch (IOException e) {
            log.debug("Could not read build file {}: {}", buildFile.getAbsolutePath(), e.getMessage());
            return true; // Assume present if we can't read the file (avoid false positive)
        }
    }

    private Finding buildFinding(String filePath) {
        return Finding.builder()
                .ruleId("SPRING012")
                .ruleName("SPRING012")
                .severity(FindingSeverity.LOW)
                .category(RuleCategory.OBSERVABILITY)
                .message("Project does not include spring-boot-starter-actuator. "
                        + "Health checks and metrics endpoints are unavailable.")
                .recommendation("Add spring-boot-starter-actuator for /actuator/health, "
                        + "/actuator/metrics, and Micrometer integration.")
                .filePath(filePath)
                .lineNumber(0)
                .columnNumber(0)
                .codeSnippet("<dependency>spring-boot-starter-actuator</dependency>")
                .build();
    }
}
