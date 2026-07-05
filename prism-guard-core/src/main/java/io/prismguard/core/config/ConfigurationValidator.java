package io.prismguard.core.config;

import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates a parsed {@link YamlConfigurationLoader.ConfigDto} before it is converted
 * into a {@link io.prismguard.api.config.PrismGuardConfiguration}.
 *
 * <h3>Validations performed</h3>
 * <ul>
 *   <li>Rule IDs are non-blank.</li>
 *   <li>Severity values (per-rule and top-level overrides) match a {@link FindingSeverity} name.</li>
 *   <li>Category values match a {@link RuleCategory} name (case-insensitive, hyphen-tolerant).</li>
 *   <li>Output format is one of the supported values.</li>
 *   <li>Parallelism, when specified, is a positive integer.</li>
 *   <li>Numeric rule parameters are positive.</li>
 *   <li>No blank keys in rule parameter map.</li>
 * </ul>
 *
 * <p>All errors are collected before throwing {@link ConfigurationValidationException}
 * so the user sees every problem in one go.
 */
@Component
public class ConfigurationValidator {

    private static final Set<String> VALID_SEVERITIES =
            Arrays.stream(FindingSeverity.values())
                    .map(Enum::name)
                    .collect(Collectors.toUnmodifiableSet());

    private static final Set<String> VALID_CATEGORIES =
            Arrays.stream(RuleCategory.values())
                    .map(Enum::name)
                    .collect(Collectors.toUnmodifiableSet());

    private static final Set<String> VALID_OUTPUT_FORMATS =
            Set.of("html");

    /**
     * Validates the configuration DTO.
     *
     * @param dto the parsed YAML DTO
     * @throws ConfigurationValidationException if any validation errors are found
     */
    public void validate(YamlConfigurationLoader.ConfigDto dto) {
        if (dto == null) return; // nothing to validate for an empty file

        List<String> errors = new ArrayList<>();

        validateOutputFormat(dto.outputFormat, errors);
        validateParallelism(dto.parallelism, errors);
        validateSeverityOverrides(dto.ruleSeverityOverrides, errors);
        validateRules(dto.rules, errors);

        if (!errors.isEmpty()) {
            throw new ConfigurationValidationException(errors);
        }
    }

    // -------------------------------------------------------------------------
    // Field validators
    // -------------------------------------------------------------------------

    private void validateOutputFormat(String outputFormat, List<String> errors) {
        if (outputFormat == null) return; // optional field
        if (!VALID_OUTPUT_FORMATS.contains(outputFormat.toLowerCase())) {
            errors.add("outputFormat: invalid value '" + outputFormat
                    + "'. Must be one of: " + VALID_OUTPUT_FORMATS
                    + ". Example: outputFormat: html");
        }
    }

    private void validateParallelism(int parallelism, List<String> errors) {
        if (parallelism < 0) {
            errors.add("parallelism: value must be 0 (auto) or a positive integer, got " + parallelism
                    + ". Example: parallelism: 4");
        }
    }

    private void validateSeverityOverrides(Map<String, String> overrides, List<String> errors) {
        if (overrides == null) return;
        overrides.forEach((ruleId, severityName) -> {
            if (ruleId == null || ruleId.isBlank()) {
                errors.add("ruleSeverityOverrides: rule ID must not be blank");
                return;
            }
            if (severityName == null || severityName.isBlank()) {
                errors.add("ruleSeverityOverrides." + ruleId
                        + ": severity value must not be blank. Must be one of: " + VALID_SEVERITIES);
                return;
            }
            if (!VALID_SEVERITIES.contains(severityName.toUpperCase())) {
                errors.add("ruleSeverityOverrides." + ruleId
                        + ": invalid severity '" + severityName
                        + "'. Must be one of: " + VALID_SEVERITIES
                        + ". Example: " + ruleId + ": HIGH");
            }
        });
    }


    private void validateRules(Map<String, YamlConfigurationLoader.RuleDto> rules, List<String> errors) {
        if (rules == null) return;
        rules.forEach((ruleId, ruleDto) -> {
            if (ruleId == null || ruleId.isBlank()) {
                errors.add("rules: rule ID must not be blank");
                return;
            }
            if (ruleDto == null) return;

            String path = "rules." + ruleId;

            // Validate severity
            if (ruleDto.severity != null && !ruleDto.severity.isBlank()) {
                if (!VALID_SEVERITIES.contains(ruleDto.severity.toUpperCase())) {
                    errors.add(path + ".severity: invalid value '" + ruleDto.severity
                            + "'. Must be one of: " + VALID_SEVERITIES
                            + ". Example: severity: HIGH");
                }
            }

            // Validate category
            if (ruleDto.category != null && !ruleDto.category.isBlank()) {
                String normalized = ruleDto.category.toUpperCase()
                        .replace('-', '_')
                        .replace(' ', '_');
                if (!VALID_CATEGORIES.contains(normalized)) {
                    errors.add(path + ".category: invalid value '" + ruleDto.category
                            + "'. Must be one of: " + VALID_CATEGORIES
                            + ". Example: category: JAVA_QUALITY");
                }
            }

            // Validate analyzer type name
            if (ruleDto.analyzer != null && ruleDto.analyzer.isBlank()) {
                errors.add(path + ".analyzer: value must not be blank when specified. "
                        + "Example: analyzer: long-method");
            }

            // Validate parameters
            if (ruleDto.parameters != null) {
                ruleDto.parameters.forEach((paramName, paramValue) -> {
                    if (paramName == null || paramName.isBlank()) {
                        errors.add(path + ".parameters: parameter name must not be blank");
                        return;
                    }
                    if (paramValue instanceof Number n && n.doubleValue() <= 0) {
                        errors.add(path + ".parameters." + paramName
                                + ": numeric value must be positive, got " + paramValue
                                + ". Example: " + paramName + ": 50");
                    }
                });
            }
        });
    }
}
