package io.prismguard.core.config;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when a {@code prismguard.yml} file fails schema or semantic validation.
 *
 * <p>Contains a list of human-readable error messages, each indicating the YAML
 * location, the problem, and a suggested correction. Prism Guard aborts the
 * analysis immediately and prints all errors before exiting.
 */
public class ConfigurationValidationException extends RuntimeException {

    private final List<String> errors;

    public ConfigurationValidationException(List<String> errors) {
        super(formatMessage(errors));
        this.errors = Collections.unmodifiableList(errors);
    }

    /** Returns all validation errors found in the configuration file. */
    public List<String> getErrors() {
        return errors;
    }

    private static String formatMessage(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return "prismguard.yml validation failed with no details.";
        }
        StringBuilder sb = new StringBuilder("prismguard.yml validation failed with ")
                .append(errors.size())
                .append(" error(s):\n");
        for (int i = 0; i < errors.size(); i++) {
            sb.append("  [").append(i + 1).append("] ").append(errors.get(i)).append('\n');
        }
        return sb.toString().trim();
    }
}
