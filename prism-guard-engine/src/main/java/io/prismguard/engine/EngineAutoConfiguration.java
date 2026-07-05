package io.prismguard.engine;

import io.prismguard.core.CoreAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot auto-configuration entry point for the {@code prism-guard-engine} module.
 *
 * <p>Activating this configuration causes Spring to:
 * <ul>
 *   <li>Component-scan the {@code io.prismguard.engine} package, picking up
 *       {@link AnalysisEngine} and any other {@code @Component}-annotated beans
 *       in this module.</li>
 *   <li>Import {@link CoreAutoConfiguration} so that the scanner, rule executor, score
 *       calculator, and other core beans are always available when the engine is used.</li>
 * </ul>
 *
 * <p>This class is registered as an auto-configuration candidate via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * so it is loaded automatically by Spring Boot's auto-configuration mechanism without
 * any explicit {@code @Import} in the host application.
 */
@Configuration
@ComponentScan("io.prismguard.engine")
@Import(CoreAutoConfiguration.class)
public class EngineAutoConfiguration {
    // Intentionally empty – all wiring is handled via annotations.
}
