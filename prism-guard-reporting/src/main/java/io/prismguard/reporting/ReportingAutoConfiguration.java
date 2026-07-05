package io.prismguard.reporting;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot auto-configuration for the Prism Guard reporting module.
 * Registers all {@link ReportGenerator} beans and {@link ReportingService}
 * via component-scan of the {@code io.prismguard.reporting} package.
 */
@AutoConfiguration
@ComponentScan("io.prismguard.reporting")
public class ReportingAutoConfiguration {
}
