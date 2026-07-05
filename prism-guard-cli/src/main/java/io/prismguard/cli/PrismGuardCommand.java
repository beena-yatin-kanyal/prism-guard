package io.prismguard.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Root Picocli command for the Prism Guard CLI.
 * When invoked without a sub-command it prints the help text.
 * Sub-commands are registered via {@link Command#subcommands}.
 */
@Component
@Command(
    name = "prism-guard",
    description = "Prism Guard - Java & Spring Boot Architecture Analyzer",
    mixinStandardHelpOptions = true,
    version = "Prism Guard 1.0.0",
    subcommands = {
        AnalyzeCommand.class,
        ListRulesCommand.class,
        InitCommand.class,
        CommandLine.HelpCommand.class
    }
)
public class PrismGuardCommand implements Runnable {

    @Override
    public void run() {
        new CommandLine(this).usage(System.out);
    }
}
