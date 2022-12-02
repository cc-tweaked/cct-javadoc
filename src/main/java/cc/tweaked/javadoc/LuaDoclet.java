/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package cc.tweaked.javadoc;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LuaDoclet implements Doclet {
    private String output = ".";
    private Path root = Path.of(".");
    private Reporter reporter;

    private final Set<Option> options = Set.of(
        new BasicOption("-d", "Set the output directory", "FILE", o -> output = o),
        new BasicOption("-project-root", "Set the directory that @source paths are generated relative to", "ROOT", o -> root = Path.of(o)),
        new BasicOption("-doctitle", "Title for the overview page", "TITLE"),
        new BasicOption("-windowtitle", "The title of the documentation", "TITLE")
    );

    @Override
    public void init(Locale locale, Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public String getName() {
        return "LuaDoclet";
    }

    @Override
    public Set<? extends Option> getSupportedOptions() {
        return options;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    @Override
    public boolean run(DocletEnvironment docEnv) {
        Environment env = Environment.of(docEnv, reporter);
        if (env == null) return false;

        Map<ExecutableElement, MethodInfo> methods = docEnv.getSpecifiedElements().stream()
            .filter(x -> x.getKind() == ElementKind.CLASS).map(TypeElement.class::cast)
            .flatMap(x -> x.getEnclosedElements().stream())

            // Only allow instance methods. Static methods are "generic peripheral" ones, and so are unsuitable.
            .filter(x -> x.getKind() == ElementKind.METHOD).map(ExecutableElement.class::cast)
            .flatMap(x -> MethodInfo.of(env, x).stream())
            .collect(Collectors.toMap(MethodInfo::element, Function.identity(), (x, y) -> {
                throw new IllegalStateException("Cannot merge terms");
            }, LinkedHashMap::new));

        Map<TypeElement, ClassInfo> types = methods.keySet().stream()
            .map(Element::getEnclosingElement)
            .filter(TypeElement.class::isInstance).map(TypeElement.class::cast)
            .distinct()
            .flatMap(x -> ClassInfo.of(env, x).stream())
            .collect(Collectors.toMap(ClassInfo::element, Function.identity()));

        try {
            new Emitter(env, methods, types, root).emit(new File(output));
            return true;
        } catch (IOException e) {
            env.message(Diagnostic.Kind.ERROR, e.getMessage());
            return false;
        }
    }

    private static class BasicOption implements Option {
        private final String name;
        private final String description;
        private final String parameter;
        private final Consumer<String> process;

        private BasicOption(String name, String description, String parameter, Consumer<String> process) {
            this.name = name;
            this.description = description;
            this.parameter = parameter;
            this.process = process;
        }

        private BasicOption(String name, String description, String parameter) {
            this(name, description, parameter, x -> {
            });
        }

        @Override
        public int getArgumentCount() {
            return 1;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public Kind getKind() {
            return Kind.STANDARD;
        }

        @Override
        public List<String> getNames() {
            return Collections.singletonList(name);
        }

        @Override
        public String getParameters() {
            return parameter;
        }

        @Override
        public boolean process(String option, List<String> arguments) {
            if (arguments.isEmpty()) return false;
            process.accept(arguments.get(0));
            return true;
        }
    }
}
