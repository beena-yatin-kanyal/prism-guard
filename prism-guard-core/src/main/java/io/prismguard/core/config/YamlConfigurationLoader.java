package io.prismguard.core.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.prismguard.api.config.PrismGuardConfiguration;
import io.prismguard.api.config.ReportConfiguration;
import io.prismguard.api.config.RuleConfig;
import io.prismguard.api.model.FindingSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads {@code prismguard.yml} (or {@code prismguard.yaml}) from the project root,
 * validates it, then builds a {@link PrismGuardConfiguration}.
 *
 */
@Component
public class YamlConfigurationLoader {

    private static final Logger log = LoggerFactory.getLogger(YamlConfigurationLoader.class);

    private static final String CONFIG_FILE_YML  = "prismguard.yml";
    private static final String CONFIG_FILE_YAML = "prismguard.yaml";

    private final ObjectMapper yamlMapper;
    private final ConfigurationValidator validator;
    private final ResourceLoader resourceLoader;

    public YamlConfigurationLoader(ConfigurationValidator validator, ResourceLoader resourceLoader) {
        this.validator = validator;
        this.resourceLoader = resourceLoader;
        this.yamlMapper = new ObjectMapper(new YAMLFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Searches the given project root for {@code prismguard.yml} or {@code prismguard.yaml}.
     */
    public PrismGuardConfiguration load(Path projectRoot) {
        Path ymlFile = projectRoot.resolve(CONFIG_FILE_YML);
        if (Files.exists(ymlFile)) {
            return buildFromMerged(parseFile(ymlFile), projectRoot);
        }
        Path yamlFile = projectRoot.resolve(CONFIG_FILE_YAML);
        if (Files.exists(yamlFile)) {
            return buildFromMerged(parseFile(yamlFile), projectRoot);
        }
        log.info("No prismguard.yml found in '{}'. Falling back to built-in default rules.", projectRoot);
        return loadBuiltinDefaults(projectRoot);
    }

    /**
     * Loads configuration from a specific file.
     */
    public PrismGuardConfiguration loadFromFile(Path configFile) {
        log.info("Loading configuration from '{}'", configFile);
        Path projectRoot = configFile.getParent() != null
                ? configFile.getParent()
                : configFile.toAbsolutePath().getParent();
        return buildFromMerged(parseFile(configFile), projectRoot);
    }

    /** Returns defaults-only configuration (no project YAML). */
    PrismGuardConfiguration defaults(Path projectRoot) {
        return buildFromMerged(new ConfigDto(), projectRoot);
    }

    /** Loads the built-in default rule set bundled inside the distribution JAR. */
    private PrismGuardConfiguration loadBuiltinDefaults(Path projectRoot) {
        try {
            Resource resource = resourceLoader.getResource("classpath:prismguard-example.yml");
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    ConfigDto dto = yamlMapper.readValue(is, ConfigDto.class);
                    log.info("Loaded {} built-in rules from classpath:prismguard-example.yml", dto.rules != null ? dto.rules.size() : 0);
                    return buildFromMerged(dto, projectRoot);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to parse built-in default config: {}", e.getMessage());
        }
        log.warn("Built-in default config not found on classpath; analysis will run with no rules.");
        return buildFromMerged(new ConfigDto(), projectRoot);
    }

    /**
     * Loads the {@link ReportConfiguration} from {@code prismguard.yml} in the project root.
     */
    public ReportConfiguration loadReportConfig(Path projectRoot) {
        Path ymlFile = projectRoot.resolve(CONFIG_FILE_YML);
        if (Files.exists(ymlFile)) {
            return buildReportConfigFromDto(parseFile(ymlFile), projectRoot);
        }
        Path yamlFile = projectRoot.resolve(CONFIG_FILE_YAML);
        if (Files.exists(yamlFile)) {
            return buildReportConfigFromDto(parseFile(yamlFile), projectRoot);
        }
        return buildReportConfigFromDto(new ConfigDto(), projectRoot);
    }

    /**
     * Loads the {@link ReportConfiguration} from a specific config file.
     */
    public ReportConfiguration loadReportConfigFromFile(Path configFile) {
        Path projectRoot = configFile.getParent() != null
                ? configFile.getParent()
                : configFile.toAbsolutePath().getParent();
        return buildReportConfigFromDto(parseFile(configFile), projectRoot);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Parses a YAML file into a {@link ConfigDto}, returning an empty DTO on failure. */
    private ConfigDto parseFile(Path configFile) {
        try {
            return yamlMapper.readValue(configFile.toFile(), ConfigDto.class);
        } catch (IOException e) {
            log.warn("Failed to parse '{}': {}", configFile, e.getMessage());
            return new ConfigDto();
        }
    }

    /** Builds a {@link PrismGuardConfiguration} from a {@link ConfigDto}. */
    private PrismGuardConfiguration buildFromMerged(ConfigDto dto, Path projectRoot) {
        // Validate before building — throws ConfigurationValidationException on errors
        validator.validate(dto);

        PrismGuardConfiguration.Builder builder = PrismGuardConfiguration.builder()
                .projectRoot(projectRoot);

        if (dto.sourceDirectories != null && !dto.sourceDirectories.isEmpty()) {
            builder.sourceDirectories(dto.sourceDirectories);
        }
        if (dto.excludePatterns != null) {
            builder.excludePatterns(dto.excludePatterns);
        }
        if (dto.disabledRules != null) {
            builder.disabledRules(new HashSet<>(dto.disabledRules));
        }
        if (dto.enabledCategories != null) {
            builder.enabledCategories(new HashSet<>(dto.enabledCategories));
        }
        if (dto.parallelism > 0) {
            builder.parallelism(dto.parallelism);
        }
        builder.failOnCritical(dto.failOnCritical);
        builder.failOnScoreBelow(dto.failOnScoreBelow);

        // Severity overrides (top-level map)
        if (dto.ruleSeverityOverrides != null) {
            Map<String, FindingSeverity> overrides = new HashMap<>();
            dto.ruleSeverityOverrides.forEach((ruleId, severityName) -> {
                try {
                    overrides.put(ruleId, FindingSeverity.valueOf(severityName.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown severity '{}' for rule '{}', ignoring", severityName, ruleId);
                }
            });
            builder.ruleSeverityOverrides(overrides);
        }

        // Per-rule RuleConfig objects
        if (dto.rules != null) {
            dto.rules.forEach((ruleId, ruleDto) -> {
                RuleConfig rc = toRuleConfig(ruleId, ruleDto, dto.ruleSeverityOverrides);
                builder.ruleConfig(ruleId, rc);
            });
        }

        // Scoring penalty model
        if (dto.scoring != null) {
            builder.scoringConfig(buildScoringConfig(dto.scoring));
        }

        // Java language level for the source parser
        if (dto.javaVersion != null && !dto.javaVersion.isBlank()) {
            builder.javaVersion(dto.javaVersion);
        }

        return builder.build();
    }

    /** Builds a {@link ReportConfiguration} from a {@link ConfigDto}. */
    private ReportConfiguration buildReportConfigFromDto(ConfigDto dto, Path projectRoot) {
        String format = dto.outputFormat != null ? dto.outputFormat : "html";
        Path directory = dto.outputDirectory != null
                ? projectRoot.resolve(dto.outputDirectory)
                : null;
        return new ReportConfiguration(format, directory);
    }

    /** Converts a {@link RuleDto} to an immutable {@link RuleConfig}. */
    private RuleConfig toRuleConfig(String ruleId, RuleDto dto,
                                    Map<String, String> severityOverrides) {
        RuleConfig.Builder rcb = RuleConfig.builder().id(ruleId);

        if (dto.title != null)          rcb.title(dto.title);
        if (dto.description != null)    rcb.description(dto.description);
        if (dto.rationale != null)      rcb.rationale(dto.rationale);
        if (dto.enabled != null)        rcb.enabled(dto.enabled);
        if (dto.recommendation != null) rcb.recommendation(dto.recommendation);
        if (dto.category != null)       rcb.category(dto.category);
        if (dto.tags != null)           rcb.tags(dto.tags);
        if (dto.analyzer != null)       rcb.analyzer(dto.analyzer);
        if (dto.parameters != null)     rcb.parameters(dto.parameters);

        // Resolve severity: explicit top-level ruleSeverityOverrides win over rule-level
        if (severityOverrides != null && severityOverrides.containsKey(ruleId)) {
            try {
                rcb.severity(FindingSeverity.valueOf(severityOverrides.get(ruleId).toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        } else if (dto.severity != null) {
            try {
                rcb.severity(FindingSeverity.valueOf(dto.severity.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown severity '{}' for rule '{}', ignoring", dto.severity, ruleId);
            }
        }

        return rcb.build();
    }

    private PrismGuardConfiguration.ScoringConfig buildScoringConfig(ScoringDto dto) {
        PrismGuardConfiguration.ScoringConfig d = PrismGuardConfiguration.ScoringConfig.defaults();
        double criticalPenalty = dto.critical != null && dto.critical.penalty != null ? dto.critical.penalty : d.getCriticalPenalty();
        double criticalCap     = dto.critical != null && dto.critical.cap     != null ? dto.critical.cap     : d.getCriticalCap();
        double highPenalty     = dto.high     != null && dto.high.penalty     != null ? dto.high.penalty     : d.getHighPenalty();
        double highCap         = dto.high     != null && dto.high.cap         != null ? dto.high.cap         : d.getHighCap();
        double mediumPenalty   = dto.medium   != null && dto.medium.penalty   != null ? dto.medium.penalty   : d.getMediumPenalty();
        double mediumCap       = dto.medium   != null && dto.medium.cap       != null ? dto.medium.cap       : d.getMediumCap();
        double lowPenalty      = dto.low      != null && dto.low.penalty      != null ? dto.low.penalty      : d.getLowPenalty();
        double lowCap          = dto.low      != null && dto.low.cap          != null ? dto.low.cap          : d.getLowCap();
        return new PrismGuardConfiguration.ScoringConfig(
                criticalPenalty, criticalCap, highPenalty, highCap,
                mediumPenalty, mediumCap, lowPenalty, lowCap);
    }

    // -------------------------------------------------------------------------
    // DTOs for YAML deserialization
    // -------------------------------------------------------------------------

    /**
     * Top-level DTO mirroring the full prismguard.yml schema.
     */
    static final class ConfigDto {
        public List<String> sourceDirectories;
        public List<String> excludePatterns;
        public Map<String, String> ruleSeverityOverrides;
        public List<String> disabledRules;
        public List<String> enabledCategories;
        public int parallelism;
        public boolean failOnCritical;
        public int failOnScoreBelow;
        public String outputFormat;
        public String outputDirectory;
        public String javaVersion;
        /** Per-rule configuration blocks keyed by rule ID. */
        public Map<String, RuleDto> rules;
        /** Optional scoring penalty model. When absent, defaults are used. */
        public ScoringDto scoring;
    }

    /**
     * DTO for the {@code scoring:} section.
     */
    static final class ScoringDto {
        public SeverityBandDto critical;
        public SeverityBandDto high;
        public SeverityBandDto medium;
        public SeverityBandDto low;
    }

    /**
     * DTO for a single severity band within {@code scoring:}.
     */
    static final class SeverityBandDto {
        public Double penalty;
        public Double cap;
    }

    /**
     * Per-rule configuration DTO.
     * All fields are optional; null means "use code defaults".
     */
    static final class RuleDto {
        /** Analyzer type name that powers this rule (e.g. {@code "long-method"}). */
        public String analyzer;
        public String title;
        public String description;
        public String rationale;
        /** Severity name, e.g. {@code "HIGH"}. */
        public String severity;
        public String category;
        public List<String> tags;
        public Boolean enabled;
        public String recommendation;
        /** Rule-specific parameters, e.g. {@code maxLines: 50}. */
        public Map<String, Object> parameters;
    }
}
