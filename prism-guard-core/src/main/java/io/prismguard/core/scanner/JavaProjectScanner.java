package io.prismguard.core.scanner;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import io.prismguard.api.config.PrismGuardConfiguration;
import io.prismguard.api.model.ClassModel;
import io.prismguard.api.model.ClassType;
import io.prismguard.api.model.FieldModel;
import io.prismguard.api.model.JavaFileModel;
import io.prismguard.api.model.MethodModel;
import io.prismguard.api.model.ModuleModel;
import io.prismguard.api.model.ProjectModel;
import io.prismguard.api.model.ProjectSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scans a project directory tree, parses all {@code .java} source files with JavaParser,
 * and assembles a {@link ProjectModel} representing the entire analysed project.
 */
@Component
public class JavaProjectScanner {

    private static final Logger log = LoggerFactory.getLogger(JavaProjectScanner.class);

    /**
     * Entry point: scan the project described by {@code configuration} and return a {@link ProjectModel}.
     */
    public ProjectModel scan(PrismGuardConfiguration configuration) {
        configureParser(configuration.getJavaVersion());
        Path projectRoot = configuration.getProjectRoot();
        String projectName = projectRoot.getFileName() != null
                ? projectRoot.getFileName().toString()
                : projectRoot.toString();

        boolean multiModule = isMultiModule(projectRoot);
        List<ModuleModel> modules = new ArrayList<>();

        if (multiModule) {
            // Scan each sub-directory that looks like a Maven/Gradle module
            try (var stream = Files.list(projectRoot)) {
                stream.filter(Files::isDirectory)
                        .filter(dir -> hasModuleBuildFile(dir))
                        .sorted()
                        .forEach(moduleDir -> {
                            String moduleName = moduleDir.getFileName().toString();
                            ModuleModel module = scanModule(moduleName, moduleDir, configuration);
                            modules.add(module);
                        });
            } catch (IOException e) {
                log.warn("Failed to list sub-modules under {}: {}", projectRoot, e.getMessage());
            }
            // Also scan root-level source directories (parent project may have sources too)
            ModuleModel rootModule = scanModule(projectName, projectRoot, configuration);
            if (rootModule.fileCount() > 0) {
                modules.add(0, rootModule);
            }
        } else {
            modules.add(scanModule(projectName, projectRoot, configuration));
        }

        ProjectSummary summary = buildSummary(projectName, projectRoot, modules, multiModule, configuration);
        return new ProjectModel(projectName, projectRoot, modules, summary);
    }

    ModuleModel scanModule(String moduleName, Path moduleRoot, PrismGuardConfiguration config) {
        List<JavaFileModel> files = new ArrayList<>();

        for (String srcDir : config.getSourceDirectories()) {
            Path sourceRoot = moduleRoot.resolve(srcDir);
            if (!Files.isDirectory(sourceRoot)) {
                continue;
            }
            try {
                Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (file.toString().endsWith(".java")) {
                            if (!matchesExcludePattern(file, config.getExcludePatterns())) {
                                JavaFileModel model = parseFile(file);
                                if (model != null) {
                                    files.add(model);
                                }
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        log.warn("Cannot access file {}: {}", file, exc.getMessage());
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                log.warn("Failed to walk source directory {}: {}", sourceRoot, e.getMessage());
            }
        }

        return new ModuleModel(moduleName, moduleRoot, files);
    }

    JavaFileModel parseFile(Path file) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);

            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("");

            List<String> imports = cu.getImports().stream()
                    .map(id -> id.getNameAsString())
                    .toList();

            List<ClassModel> classes = new ArrayList<>();

            // ClassOrInterfaceDeclaration covers classes and interfaces
            cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                    .filter(c -> !c.isInnerClass() || c.isStatic()) // include top-level and static nested
                    .forEach(c -> classes.add(buildClassModel(c, file)));

            cu.findAll(EnumDeclaration.class)
                    .forEach(e -> classes.add(buildEnumModel(e, file)));

            cu.findAll(AnnotationDeclaration.class)
                    .forEach(a -> classes.add(buildAnnotationModel(a, file)));

            cu.findAll(RecordDeclaration.class)
                    .forEach(r -> classes.add(buildRecordModel(r, file)));

            int lineCount = (int) Files.lines(file).count();

            return new JavaFileModel(file, packageName, classes, imports, lineCount);

        } catch (ParseProblemException e) {
            log.warn("Parse error in {}: {}", file, e.getMessage());
            return null;
        } catch (IOException e) {
            log.warn("IO error reading {}: {}", file, e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("Unexpected error parsing {}: {}", file, e.getMessage());
            return null;
        }
    }

    ClassModel buildClassModel(TypeDeclaration<?> typeDecl, Path sourceFile) {
        String simpleName = typeDecl.getNameAsString();
        String packageName = typeDecl.findCompilationUnit()
                .flatMap(cu -> cu.getPackageDeclaration())
                .map(pd -> pd.getNameAsString())
                .orElse("");
        String fqn = packageName.isEmpty() ? simpleName : packageName + "." + simpleName;

        ClassType classType = determineClassType(typeDecl);
        List<String> annotations = extractAnnotationNames(typeDecl.getAnnotations());

        List<MethodModel> methods = typeDecl.getMethods().stream()
                .map(this::buildMethodModel)
                .toList();

        List<FieldModel> fields = new ArrayList<>();
        typeDecl.getFields().forEach(fieldDecl ->
                fieldDecl.getVariables().forEach(var ->
                        fields.add(buildFieldModel(fieldDecl, var))));

        List<String> implementedInterfaces = List.of();
        String superClass = null;
        boolean isAbstract = false;
        boolean isPublic = typeDecl.isPublic();

        if (typeDecl instanceof ClassOrInterfaceDeclaration cid) {
            implementedInterfaces = cid.getImplementedTypes().stream()
                    .map(t -> t.getNameAsString())
                    .toList();
            superClass = cid.getExtendedTypes().stream()
                    .findFirst()
                    .map(t -> t.getNameAsString())
                    .orElse(null);
            isAbstract = cid.isAbstract() || cid.isInterface();
        }

        int beginLine = typeDecl.getBegin().map(pos -> pos.line).orElse(0);
        int endLine = typeDecl.getEnd().map(pos -> pos.line).orElse(0);

        return new ClassModel(
                simpleName, fqn, packageName, classType, annotations,
                methods, fields, implementedInterfaces, superClass,
                sourceFile, beginLine, endLine, isAbstract, isPublic);
    }

    private ClassModel buildEnumModel(EnumDeclaration enumDecl, Path sourceFile) {
        String simpleName = enumDecl.getNameAsString();
        String packageName = enumDecl.findCompilationUnit()
                .flatMap(cu -> cu.getPackageDeclaration())
                .map(pd -> pd.getNameAsString())
                .orElse("");
        String fqn = packageName.isEmpty() ? simpleName : packageName + "." + simpleName;

        List<String> annotations = extractAnnotationNames(enumDecl.getAnnotations());

        List<MethodModel> methods = enumDecl.getMethods().stream()
                .map(this::buildMethodModel)
                .toList();

        List<FieldModel> fields = new ArrayList<>();
        enumDecl.getFields().forEach(fieldDecl ->
                fieldDecl.getVariables().forEach(var ->
                        fields.add(buildFieldModel(fieldDecl, var))));

        int beginLine = enumDecl.getBegin().map(pos -> pos.line).orElse(0);
        int endLine = enumDecl.getEnd().map(pos -> pos.line).orElse(0);

        return new ClassModel(
                simpleName, fqn, packageName, ClassType.ENUM, annotations,
                methods, fields, List.of(), null,
                sourceFile, beginLine, endLine, false, enumDecl.isPublic());
    }

    private ClassModel buildAnnotationModel(AnnotationDeclaration annotDecl, Path sourceFile) {
        String simpleName = annotDecl.getNameAsString();
        String packageName = annotDecl.findCompilationUnit()
                .flatMap(cu -> cu.getPackageDeclaration())
                .map(pd -> pd.getNameAsString())
                .orElse("");
        String fqn = packageName.isEmpty() ? simpleName : packageName + "." + simpleName;

        List<String> annotations = extractAnnotationNames(annotDecl.getAnnotations());

        int beginLine = annotDecl.getBegin().map(pos -> pos.line).orElse(0);
        int endLine = annotDecl.getEnd().map(pos -> pos.line).orElse(0);

        return new ClassModel(
                simpleName, fqn, packageName, ClassType.ANNOTATION, annotations,
                List.of(), List.of(), List.of(), null,
                sourceFile, beginLine, endLine, true, annotDecl.isPublic());
    }

    private ClassModel buildRecordModel(RecordDeclaration recordDecl, Path sourceFile) {
        String simpleName = recordDecl.getNameAsString();
        String packageName = recordDecl.findCompilationUnit()
                .flatMap(cu -> cu.getPackageDeclaration())
                .map(pd -> pd.getNameAsString())
                .orElse("");
        String fqn = packageName.isEmpty() ? simpleName : packageName + "." + simpleName;

        List<String> annotations = extractAnnotationNames(recordDecl.getAnnotations());

        List<MethodModel> methods = recordDecl.getMethods().stream()
                .map(this::buildMethodModel)
                .toList();

        List<String> interfaces = recordDecl.getImplementedTypes().stream()
                .map(t -> t.getNameAsString())
                .toList();

        int beginLine = recordDecl.getBegin().map(pos -> pos.line).orElse(0);
        int endLine = recordDecl.getEnd().map(pos -> pos.line).orElse(0);

        return new ClassModel(
                simpleName, fqn, packageName, ClassType.RECORD, annotations,
                methods, List.of(), interfaces, null,
                sourceFile, beginLine, endLine, false, recordDecl.isPublic());
    }

    MethodModel buildMethodModel(MethodDeclaration method) {
        String name = method.getNameAsString();
        String returnType = method.getType().asString();

        List<String> paramTypes = method.getParameters().stream()
                .map(p -> p.getType().asString())
                .toList();

        List<String> annotations = extractAnnotationNames(method.getAnnotations());

        boolean isPublic = method.isPublic();
        boolean isStatic = method.isStatic();
        boolean isAbstract = method.isAbstract();

        int beginLine = method.getBegin().map(pos -> pos.line).orElse(0);
        int endLine = method.getEnd().map(pos -> pos.line).orElse(0);

        int cc = computeCyclomaticComplexity(method);

        return new MethodModel(name, returnType, paramTypes, annotations,
                isPublic, isStatic, isAbstract, beginLine, endLine, cc);
    }

    int computeCyclomaticComplexity(MethodDeclaration method) {
        AtomicInteger complexity = new AtomicInteger(1);

        method.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(IfStmt n, Void arg) {
                complexity.incrementAndGet();
                super.visit(n, arg);
            }

            @Override
            public void visit(ForStmt n, Void arg) {
                complexity.incrementAndGet();
                super.visit(n, arg);
            }

            @Override
            public void visit(ForEachStmt n, Void arg) {
                complexity.incrementAndGet();
                super.visit(n, arg);
            }

            @Override
            public void visit(WhileStmt n, Void arg) {
                complexity.incrementAndGet();
                super.visit(n, arg);
            }

            @Override
            public void visit(DoStmt n, Void arg) {
                complexity.incrementAndGet();
                super.visit(n, arg);
            }

            @Override
            public void visit(SwitchEntry n, Void arg) {
                // Only count non-default entries as branches
                if (!n.getLabels().isEmpty()) {
                    complexity.incrementAndGet();
                }
                super.visit(n, arg);
            }

            @Override
            public void visit(CatchClause n, Void arg) {
                complexity.incrementAndGet();
                super.visit(n, arg);
            }

            @Override
            public void visit(BinaryExpr n, Void arg) {
                BinaryExpr.Operator op = n.getOperator();
                if (op == BinaryExpr.Operator.AND || op == BinaryExpr.Operator.OR) {
                    complexity.incrementAndGet();
                }
                super.visit(n, arg);
            }
        }, null);

        return complexity.get();
    }

    FieldModel buildFieldModel(FieldDeclaration field, VariableDeclarator var) {
        String name = var.getNameAsString();
        String type = var.getTypeAsString();
        List<String> annotations = extractAnnotationNames(field.getAnnotations());
        boolean isPrivate = field.isPrivate();
        boolean isStatic = field.isStatic();
        boolean isFinal = field.isFinal();
        return new FieldModel(name, type, annotations, isPrivate, isStatic, isFinal);
    }

    List<String> extractAnnotationNames(NodeList<AnnotationExpr> annotations) {
        return annotations.stream()
                .map(AnnotationExpr::getNameAsString)
                .toList();
    }

    boolean matchesExcludePattern(Path file, List<String> excludePatterns) {
        if (excludePatterns == null || excludePatterns.isEmpty()) return false;
        String filePath = file.toString().replace('\\', '/');
        for (String pattern : excludePatterns) {
            try {
                PathMatcher matcher = FileSystems.getDefault()
                        .getPathMatcher("glob:" + pattern);
                if (matcher.matches(file) || filePath.contains(pattern)) {
                    return true;
                }
            } catch (Exception e) {
                // If pattern is invalid, fall back to simple string containment
                if (filePath.contains(pattern)) return true;
            }
        }
        return false;
    }

    private ClassType determineClassType(TypeDeclaration<?> typeDecl) {
        if (typeDecl instanceof ClassOrInterfaceDeclaration cid) {
            return cid.isInterface() ? ClassType.INTERFACE : ClassType.CLASS;
        }
        if (typeDecl instanceof EnumDeclaration) return ClassType.ENUM;
        if (typeDecl instanceof AnnotationDeclaration) return ClassType.ANNOTATION;
        if (typeDecl instanceof RecordDeclaration) return ClassType.RECORD;
        return ClassType.CLASS;
    }

    private boolean isMultiModule(Path projectRoot) {
        // Check if direct children have their own pom.xml or build.gradle
        try (var stream = Files.list(projectRoot)) {
            return stream.filter(Files::isDirectory)
                    .anyMatch(this::hasModuleBuildFile);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean hasModuleBuildFile(Path dir) {
        return Files.exists(dir.resolve("pom.xml"))
                || Files.exists(dir.resolve("build.gradle"))
                || Files.exists(dir.resolve("build.gradle.kts"));
    }

    private ProjectSummary buildSummary(
            String projectName,
            Path projectRoot,
            List<ModuleModel> modules,
            boolean multiModule,
            PrismGuardConfiguration config) {

        int totalFiles = modules.stream().mapToInt(ModuleModel::fileCount).sum();
        int totalClasses = modules.stream().mapToInt(ModuleModel::classCount).sum();
        int totalMethods = modules.stream()
                .flatMap(ModuleModel::allClasses)
                .mapToInt(c -> c.getMethods().size())
                .sum();
        int totalLines = modules.stream()
                .flatMap(m -> m.getFiles().stream())
                .mapToInt(JavaFileModel::getLineCount)
                .sum();

        List<String> moduleNames = modules.stream().map(ModuleModel::getName).toList();

        boolean hasMaven = Files.exists(projectRoot.resolve("pom.xml"));
        boolean hasGradle = Files.exists(projectRoot.resolve("build.gradle"))
                || Files.exists(projectRoot.resolve("build.gradle.kts"));

        boolean hasSpring = modules.stream()
                .flatMap(ModuleModel::allClasses)
                .anyMatch(ClassModel::isSpringComponent);

        List<String> detectedFrameworks = new ArrayList<>();
        if (hasSpring) detectedFrameworks.add("Spring Boot");

        String javaVersion = detectJavaVersion(projectRoot);

        return new ProjectSummary(
                projectName,
                projectRoot,
                totalFiles,
                totalClasses,
                totalMethods,
                totalLines,
                moduleNames,
                detectedFrameworks,
                multiModule,
                hasSpring,
                hasMaven,
                hasGradle,
                javaVersion);
    }

    /**
     * Configures {@link StaticJavaParser} with the language level matching {@code javaVersion}.
     * Accepts short forms ("8", "11", "17", "21") and long forms ("1.8", "17.0.1").
     * Defaults to {@link ParserConfiguration.LanguageLevel#JAVA_21} for unrecognised values.
     */
    private void configureParser(String javaVersion) {
        ParserConfiguration.LanguageLevel level = resolveLanguageLevel(javaVersion);
        StaticJavaParser.setConfiguration(new ParserConfiguration().setLanguageLevel(level));
        log.debug("JavaParser language level set to {} (javaVersion='{}')", level, javaVersion);
    }

    private ParserConfiguration.LanguageLevel resolveLanguageLevel(String javaVersion) {
        if (javaVersion == null || javaVersion.isBlank()) {
            return ParserConfiguration.LanguageLevel.JAVA_21;
        }
        // Normalise "1.8" → "8", "17.0.1" → "17"
        String v = javaVersion.trim();
        if (v.startsWith("1.")) v = v.substring(2);
        int dotIdx = v.indexOf('.');
        if (dotIdx > 0) v = v.substring(0, dotIdx);

        return switch (v) {
            case "8"  -> ParserConfiguration.LanguageLevel.JAVA_8;
            case "11" -> ParserConfiguration.LanguageLevel.JAVA_11;
            case "14" -> ParserConfiguration.LanguageLevel.JAVA_14;
            case "15" -> ParserConfiguration.LanguageLevel.JAVA_15;
            case "16" -> ParserConfiguration.LanguageLevel.JAVA_16;
            case "17" -> ParserConfiguration.LanguageLevel.JAVA_17;
            case "18" -> ParserConfiguration.LanguageLevel.JAVA_18;
            case "19" -> ParserConfiguration.LanguageLevel.JAVA_19;
            case "20" -> ParserConfiguration.LanguageLevel.JAVA_20;
            case "21" -> ParserConfiguration.LanguageLevel.JAVA_21;
            default -> {
                log.warn("Unrecognised javaVersion '{}', defaulting parser to JAVA_21", javaVersion);
                yield ParserConfiguration.LanguageLevel.JAVA_21;
            }
        };
    }

    private String detectJavaVersion(Path projectRoot) {
        // Try to read the java.version or maven.compiler.source from pom.xml naively
        Path pom = projectRoot.resolve("pom.xml");
        if (Files.exists(pom)) {
            try {
                String content = Files.readString(pom);
                // Simple heuristic extraction — full XML parsing is not a dependency here
                int idx = content.indexOf("<java.version>");
                if (idx >= 0) {
                    int end = content.indexOf("</java.version>", idx);
                    if (end > idx) {
                        return content.substring(idx + "<java.version>".length(), end).trim();
                    }
                }
                idx = content.indexOf("<maven.compiler.source>");
                if (idx >= 0) {
                    int end = content.indexOf("</maven.compiler.source>", idx);
                    if (end > idx) {
                        return content.substring(idx + "<maven.compiler.source>".length(), end).trim();
                    }
                }
            } catch (IOException e) {
                log.debug("Could not read pom.xml for java version detection: {}", e.getMessage());
            }
        }
        return System.getProperty("java.version", "unknown");
    }
}
