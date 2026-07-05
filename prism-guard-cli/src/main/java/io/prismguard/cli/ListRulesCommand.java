package io.prismguard.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.prismguard.api.config.PrismGuardConfiguration;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.Rule;
import io.prismguard.core.config.YamlConfigurationLoader;
import io.prismguard.core.engine.RuleRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sub-command that lists all registered analysis rules in a formatted table or as JSON.
 *
 * <p>Rules are resolved from the project's {@code prismguard.yml}, so the output reflects
 * exactly the rules that would run during an analysis of that project.
 */
@Component
@Command(
    name = "list-rules",
    description = "List all available analysis rules",
    mixinStandardHelpOptions = true
)
public class ListRulesCommand implements Runnable {

    @Parameters(
        index = "0",
        description = "Path to the project root directory (default: current directory)",
        defaultValue = "."
    )
    private Path projectPath;

    @Option(
        names = {"-c", "--config"},
        description = "Path to a prismguard.yml config file (default: <project>/prismguard.yml)"
    )
    private Path configFile;

    @Option(
        names = {"--category"},
        description = "Filter by rule category (e.g. ARCHITECTURE, JAVA_QUALITY, SPRING_BOOT)"
    )
    private String category;

    @Option(
        names = {"--json"},
        description = "Output rules as a JSON array"
    )
    private boolean json;

    @Autowired
    private RuleRegistry ruleRegistry;

    @Autowired
    private YamlConfigurationLoader configLoader;

    @Override
    public void run() {
        List<Rule> rules = resolveRules();

        if (json) {
            printJson(rules);
        } else {
            printTable(rules);
        }
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    private void printTable(List<Rule> rules) {
        System.out.printf("%-12s %-48s %-10s %-30s%n", "ID", "Name", "Severity", "Category");
        System.out.println("─".repeat(102));

        rules.forEach(rule -> {
            var rc = rule.ruleConfig();
            System.out.printf("%-12s %-48s %-10s %-30s%n",
                rc.getId(),
                truncate(rc.getTitle() != null ? rc.getTitle() : rc.getId(), 47),
                rc.getSeverity() != null ? rc.getSeverity().getDisplayName() : "—",
                rc.getCategory() != null ? rc.getCategory() : "—");
        });

        System.out.println();
        System.out.println("Total: " + rules.size() + " rule(s)");
    }

    private void printJson(List<Rule> rules) {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        List<Map<String, Object>> ruleList = rules.stream().map(rule -> {
            var rc = rule.ruleConfig();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",          rc.getId());
            m.put("name",        rc.getTitle() != null ? rc.getTitle() : rc.getId());
            m.put("description", rc.getDescription());
            m.put("severity",    rc.getSeverity() != null ? rc.getSeverity().name() : null);
            m.put("category",    rc.getCategory());
            return m;
        }).collect(Collectors.toList());

        try {
            System.out.println(mapper.writeValueAsString(ruleList));
        } catch (IOException e) {
            System.err.println("Failed to serialize rules to JSON: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<Rule> resolveRules() {
        Path resolvedProject = projectPath.toAbsolutePath().normalize();
        PrismGuardConfiguration config = configFile != null
            ? configLoader.loadFromFile(configFile.toAbsolutePath().normalize())
            : configLoader.load(resolvedProject);

        List<Rule> enabled = ruleRegistry.getEnabledRules(config);

        if (category != null && !category.isBlank()) {
            try {
                RuleCategory cat = RuleCategory.valueOf(category.toUpperCase().trim());
                return enabled.stream()
                    .filter(r -> {
                        String ruleCat = r.ruleConfig().getCategory();
                        if (ruleCat == null) return false;
                        String normalized = ruleCat.toUpperCase().replace('-', '_').replace(' ', '_');
                        return cat.name().equals(normalized);
                    })
                    .toList();
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown category '" + category + "'. " +
                    "Valid values: " + java.util.Arrays.toString(RuleCategory.values()));
                return List.of();
            }
        }

        return enabled;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 1) + "…";
    }
}
