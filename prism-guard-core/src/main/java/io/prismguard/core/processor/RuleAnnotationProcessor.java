package io.prismguard.core.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Compile-time annotation processor for {@link io.prismguard.api.annotation.Analyzer}.
 *
 * <p>Scans all classes annotated with {@code @Analyzer} and writes their
 * fully-qualified names into {@code META-INF/prismguard/analyzers} in the module's
 * compile output. At runtime {@link io.prismguard.core.engine.AnalyzerRegistry} reads
 * all copies of this file from every JAR on the classpath, instantiates each class via
 * its public no-arg constructor, and builds the type-name → instance map used by
 * {@link io.prismguard.core.engine.RuleRegistry}.
 *
 * <p><b>Contributor workflow:</b> Add a new analyzer class annotated with
 * {@code @Analyzer(type = "my-check")} and a {@code @Check} method — the processor
 * registers it automatically. No provider list, no Spring bean, no service file required.
 */
@SupportedAnnotationTypes("io.prismguard.api.annotation.Analyzer")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class RuleAnnotationProcessor extends AbstractProcessor {

    private static final String ANALYZER_SERVICE_FILE = "META-INF/prismguard/analyzers";

    private final Set<String> discoveredAnalyzers = new LinkedHashSet<>();
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element.getKind() == ElementKind.CLASS) {
                    discoveredAnalyzers.add(((TypeElement) element).getQualifiedName().toString());
                }
            }
        }

        if (roundEnv.processingOver() && !discoveredAnalyzers.isEmpty()) {
            writeServiceFile(ANALYZER_SERVICE_FILE, discoveredAnalyzers);
        }

        return false;
    }

    private void writeServiceFile(String fileName, Set<String> classNames) {
        try {
            FileObject file = filer.createResource(
                    StandardLocation.CLASS_OUTPUT, "", fileName);
            try (PrintWriter writer = new PrintWriter(file.openWriter())) {
                classNames.forEach(writer::println);
            }
            messager.printMessage(Diagnostic.Kind.NOTE,
                    "RuleAnnotationProcessor: registered " + classNames.size()
                            + " analyzer(s) in " + fileName);
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "RuleAnnotationProcessor: failed to write " + fileName
                            + ": " + e.getMessage());
        }
    }
}
