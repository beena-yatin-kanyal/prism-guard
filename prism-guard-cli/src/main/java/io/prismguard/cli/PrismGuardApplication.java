package io.prismguard.cli;

import org.fusesource.jansi.AnsiConsole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;

/**
 * Entry point for the Prism Guard CLI application.
 * <p>
 * Spring Boot is configured as a non-web application. All command-line interaction
 * is delegated to Picocli. The {@link CommandLine.IFactory} provided by
 * {@code picocli-spring-boot-starter} ensures that every command and subcommand
 * instance is resolved from the Spring application context, so {@code @Autowired}
 * dependencies are fully injected.
 * <p>
 * Jansi ANSI support is installed before Spring context startup so that all
 * downstream components can emit coloured output on any platform.
 */
@SpringBootApplication
public class PrismGuardApplication implements CommandLineRunner, ExitCodeGenerator {

    @Autowired
    private PrismGuardCommand prismGuardCommand;

    @Autowired
    private CommandLine.IFactory factory;

    private int exitCode = 0;

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        int code;
        try {
            code = SpringApplication.exit(
                    SpringApplication.run(PrismGuardApplication.class, args));
        } finally {
            AnsiConsole.systemUninstall();
        }
        System.exit(code);
    }

    @Override
    public void run(String... args) {
        exitCode = new CommandLine(prismGuardCommand, factory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
