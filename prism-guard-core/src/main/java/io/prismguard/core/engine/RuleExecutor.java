package io.prismguard.core.engine;

import io.prismguard.api.model.RuleResult;
import io.prismguard.api.rule.Rule;
import io.prismguard.api.rule.RuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Executes rules against a {@link RuleContext} using Java 21 virtual threads.
 * Each rule is isolated: a failure in one rule never prevents others from running.
 */
@Component
public class RuleExecutor {

    private static final Logger log = LoggerFactory.getLogger(RuleExecutor.class);

    /**
     * Default per-rule timeout.
     */
    private static final Duration DEFAULT_RULE_TIMEOUT = Duration.ofSeconds(30);

    private final RuleRegistry registry;

    public RuleExecutor(RuleRegistry registry) {
        this.registry = registry;
    }

    /**
     * Executes all enabled rules in parallel using Java 21 virtual threads.
     * Returns results in the order rules complete.
     */
    public List<RuleResult> executeAll(RuleContext context) {
        List<Rule> rules = registry.getEnabledRules(context.configuration()).stream()
                .filter(r -> r.isApplicable(context.project()))
                .toList();

        log.info("Executing {} rules in parallel (virtual threads)", rules.size());

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<RuleResult>> futures = rules.stream()
                    .map(rule -> CompletableFuture.supplyAsync(
                            () -> executeWithTimeout(rule, context, DEFAULT_RULE_TIMEOUT),
                            executor))
                    .toList();

            return futures.stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.error("Rule execution interrupted", e);
                            return null;
                        } catch (ExecutionException e) {
                            log.error("Unexpected error retrieving rule result", e.getCause());
                            return null;
                        }
                    })
                    .filter(result -> result != null)
                    .toList();
        }
    }

    /**
     * Executes a single rule with a hard timeout. Returns a failure result if the rule
     * times out, throws, or is interrupted.
     */
    RuleResult executeWithTimeout(Rule rule, RuleContext context, Duration timeout) {
        Instant start = Instant.now();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var future = executor.submit(() -> rule.analyze(context));
            try {
                RuleResult result = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                return result;
            } catch (TimeoutException e) {
                future.cancel(true);
                Duration elapsed = Duration.between(start, Instant.now());
                log.warn("Rule '{}' timed out after {}ms", rule.ruleConfig().getId(), elapsed.toMillis());
                return RuleResult.failure(rule.ruleConfig(), elapsed,
                        "Rule timed out after " + timeout.toSeconds() + " seconds");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Duration elapsed = Duration.between(start, Instant.now());
                return RuleResult.failure(rule.ruleConfig(), elapsed, "Execution interrupted");
            } catch (ExecutionException e) {
                Duration elapsed = Duration.between(start, Instant.now());
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                log.warn("Rule '{}' threw an exception: {}", rule.ruleConfig().getId(), cause.getMessage(), cause);
                return RuleResult.failure(rule.ruleConfig(), elapsed, cause.getMessage());
            }
        }
    }
}
