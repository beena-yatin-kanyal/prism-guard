package io.prismguard.analyzers.architecture;

import io.prismguard.api.annotation.Check;
import io.prismguard.api.annotation.Analyzer;
import io.prismguard.api.model.Finding;
import io.prismguard.api.model.FindingSeverity;
import io.prismguard.api.model.RuleCategory;
import io.prismguard.api.rule.RuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ARCH002 — Circular Dependency.
 *
 * Builds a package-level dependency graph from import statements and detects
 * cycles using iterative DFS with a visited/in-stack marker.
 *
 * Only intra-project packages are considered (packages whose prefix appears in
 * at least one source file of the project). Each package is abbreviated to its
 * first three segments to keep the graph manageable.
 */
@Analyzer(type = "circular-dependency")
public class CircularDependencyRule {

    private static final Logger log = LoggerFactory.getLogger(CircularDependencyRule.class);

    @Check
    public List<Finding> check(RuleContext context) {
        int packageDepth = context.parameter("packageDepth", 3);

        List<Finding> findings = new ArrayList<>();

        // Step 1: Collect all intra-project package prefixes
        Set<String> projectPackages = context.project().allFiles()
                .map(f -> truncatePackage(f.getPackageName(), packageDepth))
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (projectPackages.isEmpty()) {
            return findings;
        }

        // Step 2: Build adjacency map — package → set of packages it imports from
        Map<String, Set<String>> adjacency = new HashMap<>();
        for (String pkg : projectPackages) {
            adjacency.put(pkg, new HashSet<>());
        }

        context.project().allFiles().forEach(file -> {
            String sourcePackage = truncatePackage(file.getPackageName(), packageDepth);
            if (sourcePackage == null || !projectPackages.contains(sourcePackage)) {
                return;
            }
            for (String importedClass : file.getImports()) {
                String importedPackage = packageOfImport(importedClass);
                String truncatedImported = truncatePackage(importedPackage, packageDepth);
                if (truncatedImported != null
                        && projectPackages.contains(truncatedImported)
                        && !truncatedImported.equals(sourcePackage)) {
                    adjacency.get(sourcePackage).add(truncatedImported);
                }
            }
        });

        // Step 3: Detect cycles using iterative DFS
        Set<String> visited = new HashSet<>();
        Set<String> reported = new HashSet<>();

        for (String startNode : projectPackages) {
            if (!visited.contains(startNode)) {
                detectCycles(startNode, adjacency, visited, reported, findings, context);
            }
        }

        return findings;
    }

    /**
     * Iterative DFS cycle detection using an explicit stack.
     * Each stack frame carries: (node, iterator-over-neighbours, path-so-far).
     */
    private void detectCycles(
            String startNode,
            Map<String, Set<String>> adjacency,
            Set<String> visited,
            Set<String> reported,
            List<Finding> findings,
            RuleContext context) {

        // Stack entries: [current node, path from start to current (inclusive)]
        Deque<Object[]> stack = new ArrayDeque<>();
        // in-stack set for cycle detection
        Set<String> inStack = new LinkedHashSet<>();

        stack.push(new Object[]{startNode, new ArrayList<String>()});

        while (!stack.isEmpty()) {
            Object[] frame = stack.peek();
            String node = (String) frame[0];
            @SuppressWarnings("unchecked")
            List<String> path = (List<String>) frame[1];

            if (!path.contains(node)) {
                // First visit to this node in this DFS path
                visited.add(node);
                inStack.add(node);
                path.add(node);

                // Push neighbours
                Set<String> neighbours = adjacency.getOrDefault(node, Set.of());
                boolean pushedAny = false;
                for (String neighbour : neighbours) {
                    if (inStack.contains(neighbour)) {
                        // Found a cycle — extract the cycle segment
                        int cycleStart = path.indexOf(neighbour);
                        List<String> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
                        cycle.add(neighbour); // close the cycle

                        String cycleKey = normalizeCycleKey(cycle);
                        if (!reported.contains(cycleKey)) {
                            reported.add(cycleKey);
                            String cycleDescription = String.join(" → ", cycle);
                            String projectRoot = context.configuration().getProjectRoot() != null
                                    ? context.configuration().getProjectRoot().toString()
                                    : "";
                            findings.add(Finding.builder()
                                    .ruleId("ARCH002")
                                    .ruleName("ARCH002")
                                    .severity(FindingSeverity.CRITICAL)
                                    .category(RuleCategory.ARCHITECTURE)
                                    .message(String.format("Circular dependency detected: %s", cycleDescription))
                                    .recommendation("Break the cycle by introducing an abstraction (interface/event bus) or restructuring packages.")
                                    .filePath(projectRoot)
                                    .lineNumber(0)
                                    .columnNumber(0)
                                    .codeSnippet(cycleDescription)
                                    .build());
                        }
                    } else if (!visited.contains(neighbour) && adjacency.containsKey(neighbour)) {
                        List<String> newPath = new ArrayList<>(path);
                        stack.push(new Object[]{neighbour, newPath});
                        pushedAny = true;
                        break; // process one neighbour at a time (depth-first)
                    }
                }

                if (!pushedAny) {
                    // All neighbours processed — pop and backtrack
                    stack.pop();
                    inStack.remove(node);
                }
            } else {
                // Already processed this node in current path — backtrack
                stack.pop();
                inStack.remove(node);
            }
        }
    }

    /**
     * Produces a canonical (rotation-normalised) string key for a cycle so that
     * the same cycle found from different starting nodes is de-duplicated.
     */
    private String normalizeCycleKey(List<String> cycle) {
        if (cycle.isEmpty()) {
            return "";
        }
        // Find the lexicographically smallest element and rotate the cycle to start there
        int minIdx = 0;
        for (int i = 1; i < cycle.size() - 1; i++) { // -1 because last == first
            if (cycle.get(i).compareTo(cycle.get(minIdx)) < 0) {
                minIdx = i;
            }
        }
        List<String> normalised = new ArrayList<>();
        for (int i = 0; i < cycle.size() - 1; i++) {
            normalised.add(cycle.get((minIdx + i) % (cycle.size() - 1)));
        }
        return String.join("->", normalised);
    }

    /**
     * Truncates a package name to {@code packageDepth} segments.
     * Returns {@code null} for blank input.
     */
    private String truncatePackage(String packageName, int packageDepth) {
        if (packageName == null || packageName.isBlank()) {
            return null;
        }
        String[] parts = packageName.split("\\.");
        if (parts.length <= packageDepth) {
            return packageName;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < packageDepth; i++) {
            if (i > 0) sb.append('.');
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    /**
     * Extracts the package name from a fully-qualified class import.
     * e.g. {@code io.example.service.UserService} → {@code io.example.service}
     */
    private String packageOfImport(String importedClass) {
        int lastDot = importedClass.lastIndexOf('.');
        if (lastDot <= 0) {
            return importedClass;
        }
        return importedClass.substring(0, lastDot);
    }
}
