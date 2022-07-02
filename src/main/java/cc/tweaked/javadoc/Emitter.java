/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package cc.tweaked.javadoc;

import com.google.auto.common.MoreElements;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.util.DocTrees;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Emitter {
    private final Map<ClassInfo, String> typeBuilders = new HashMap<>();
    private final List<MethodCollection> methodBuilders;

    private final Environment env;
    private final Map<TypeElement, ClassInfo> types;
    private final Map<ExecutableElement, MethodInfo> methods;
    private final Path root = Paths.get(".").toAbsolutePath();

    public Emitter(Environment env, Map<ExecutableElement, MethodInfo> methods, Map<TypeElement, ClassInfo> types) {
        this.env = env;
        this.types = types;
        this.methods = methods;

        Map<Element, List<MethodInfo>> methodsGroups = new HashMap<>();
        for (MethodInfo method : methods.values()) {
            methodsGroups
                .computeIfAbsent(method.element().getEnclosingElement(), k -> new ArrayList<>())
                .add(method);
        }
        methodBuilders = methodsGroups.entrySet().stream()
            .map(pair -> {
                ClassInfo klass = resolveType(pair.getKey());
                // Methods are sorted by their position within the file.
                List<EmittedMethod> emitted = pair.getValue().stream()
                    .map(method -> methodBuilder(klass, method))
                    .sorted(Comparator.comparing(x -> getPosition(x.method.element())))
                    .collect(Collectors.toList());
                return new MethodCollection(klass, pair.getKey(), emitted);
            })
            .sorted((x, y) -> {
                // Classes are sorted by depth in the type hierarchy.
                Types tys = env.types();
                if (tys.isAssignable(x.type, y.type)) return -1;
                if (tys.isAssignable(y.type, x.type)) return 1;

                return x.enclosing.getSimpleName().toString().compareTo(y.enclosing.getSimpleName().toString());
            })
            .collect(Collectors.toList());

        for (ClassInfo type : types.values()) typeBuilders.put(type, classBuilder(type));

        methodBuilders.stream().flatMap(x -> x.methods.stream()).filter(x -> !x.isUsed()).forEach(e -> {
            MethodInfo info = e.method;
            env.message(Diagnostic.Kind.NOTE, "Cannot find owner for " + info.name(), info.element());
        });
    }

    @Nullable
    private ClassInfo resolveType(@Nullable Element type) {
        while (true) {
            if (type == null || type.getKind() != ElementKind.CLASS) return null;
            TypeElement tyElem = (TypeElement) type;

            ClassInfo info = types.get(tyElem);
            if (info != null) return info;

            type = env.types().asElement(tyElem.getSuperclass());
        }
    }

    @Nonnull
    private String classBuilder(@Nonnull ClassInfo info) {
        StringBuilder builder = new StringBuilder();

        if (info.kind() == ClassInfo.Kind.TYPE) {
            builder.append("--- @module ").append(info.moduleName()).append("\n\n");
        }

        builder.append("--[[- ");
        new DocConverter(env, info.element(), x -> resolve(info, x)).visit(info.doc(), builder);

        writeSource(builder, info.element());

        switch (info.kind()) {
            case PERIPHERAL:
                builder.append("@module[kind=peripheral] ").append(info.name()).append("\n]]\n");
                break;
            case GENERIC:
                builder.append("@module[kind=generic_peripheral] ").append(info.name()).append("\n]]\n");
                break;
            case API:
                builder.append("@module[module] ").append(info.name()).append("\n]]\n");
                break;
            case TYPE:
                builder.append("@type ").append(info.typeName()).append("\n]]\n");
                builder.append("local ").append(info.typeName()).append(" = {}\n");
                break;
            default:
                throw new IllegalStateException("Unknown kind " + info.kind());
        }

        String prefix = info.typeName() == null ? "" : info.typeName() + ".";
        for (MethodCollection collection : methodBuilders) {
            if (collection.appearsIn(info)) collection.emit(prefix, builder);
        }

        return builder.toString();
    }

    @Nonnull
    private EmittedMethod methodBuilder(@Nullable ClassInfo klass, @Nonnull MethodInfo info) {
        ExecutableElement method = info.element();

        boolean isStatic = method.getModifiers().contains(Modifier.STATIC);
        if (klass != null) {
            boolean isGeneric = klass.kind() == ClassInfo.Kind.GENERIC;
            if (isStatic && !isGeneric) {
                env.message(Diagnostic.Kind.ERROR, "Cannot have static methods on non-generic sources", method);
            } else if (isGeneric && !isStatic) {
                env.message(Diagnostic.Kind.ERROR, "Cannot have instance methods on generic sources", method);
            }
        }

        DocConverter doc = new DocConverter(env, method, x -> resolve(klass, x));
        TypeConverter type = new TypeConverter(env, method);

        StringBuilder builder = new StringBuilder();
        builder.append("--[[- ");
        doc.visit(info.doc(), builder);
        builder.append("\n");
        writeSource(builder, method);

        String signature;
        if (!doc.hasParam()) {
            List<? extends VariableElement> arguments = method.getParameters();
            if (isStatic) arguments = arguments.subList(1, arguments.size());

            List<String> parameters = new ArrayList<>();
            for (VariableElement element : arguments) {
                String name = argBuilder(builder, doc, element);
                if (name != null) parameters.add(name);
            }
            signature = String.join(", ", parameters);
        } else {
            boolean hasAny = method.getParameters().stream()
                .map(Element::asType)
                .anyMatch(x -> !Helpers.isIrrelevant(x) && !Helpers.isKnown(x));

            if (!hasAny) {
                doc.message(Diagnostic.Kind.WARNING, "Method uses @cc.tparam, but has no arbitrary arguments.", method);
            }
            signature = "";
        }

        boolean hasAny = doc.hasReturn();
        if (!hasAny && Helpers.isAny(method.getReturnType())) {
            doc.message(Diagnostic.Kind.WARNING, "Method returns an arbitrary object but has no @cc.return tag.", method);
        } else if (hasAny && Helpers.isKnown(method.getReturnType())) {
            doc.message(Diagnostic.Kind.WARNING, "Method has a @cc.return but returns a known type.", method);
        }

        // If we've no explicit @cc.return annotation, then extract it from the @return tag.
        if (!doc.hasReturn() && method.getReturnType().getKind() != TypeKind.VOID) {
            builder.append("@treturn ");
            type.visit(method.getReturnType(), builder);
            if (method.getAnnotation(Nullable.class) != null) builder.append("|nil");
            builder.append(" ");
            doc.visit(doc.getReturns(), builder);
            builder.append("\n");
        }

        builder.append("]]\n");

        return new EmittedMethod(info, builder.toString(), signature);
    }

    @Nullable
    private String argBuilder(StringBuilder builder, DocConverter docs, VariableElement element) {
        TypeMirror type = element.asType();
        if (Helpers.isIrrelevant(type)) return null;

        if (Helpers.isAny(type)) {
            docs.message(Diagnostic.Kind.WARNING, "Method has a dynamic argument but has no @cc.param tag.", element);
            return "...";
        }

        String name = element.getSimpleName().toString();
        String prettyName;
        if (name.endsWith("A")) {
            prettyName = name.substring(0, name.length() - 1);
        } else if (name.endsWith("Arg")) {
            prettyName = name.substring(0, name.length() - 3);
        } else {
            prettyName = name;
        }

        TypeMirror optional = Helpers.unwrapOptional(type);

        builder.append("@tparam");
        if (optional != null) builder.append("[opt]");
        builder.append(" ");

        new TypeConverter(env, element).visit(optional == null ? type : optional, builder);
        builder.append(" ").append(prettyName).append(" ");
        docs.visit(docs.getParams().get(name), builder);
        builder.append("\n");

        return name;
    }

    public void emit(@Nonnull File output) throws IOException {
        if (!output.exists() && !output.mkdirs()) throw new IOException("Cannot create output directory: " + output);

        for (Map.Entry<ClassInfo, String> module : typeBuilders.entrySet()) {
            if (module.getKey().isHidden()) continue;

            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(output, module.getKey().name() + ".lua")), StandardCharsets.UTF_8))) {
                writer.write(module.getValue());
            }
        }
    }

    @Nullable
    private String resolve(@Nullable ClassInfo context, Element element) {
        switch (element.getKind()) {
            case CLASS: {
                ClassInfo type = types.get(MoreElements.asType(element));
                return type == null ? null : type.name();
            }

            case METHOD: {
                MethodInfo method = methods.get(MoreElements.asExecutable(element));
                ClassInfo builder = resolveType(method.element().getEnclosingElement());
                if (builder == null) return null;
                return builder == context ? method.name() : builder.name() + "." + method.name();
            }

            default:
                return null;
        }
    }

    private long getPosition(Element element) {
        DocTrees trees = env.trees();
        CompilationUnitTree tree = trees.getPath(element).getCompilationUnit();
        return trees.getSourcePositions().getStartPosition(tree, trees.getTree(element));
    }

    private void writeSource(StringBuilder builder, Element element) {
        CompilationUnitTree tree = env.trees().getPath(element).getCompilationUnit();
        LineMap map = tree.getLineMap();
        long position = getPosition(element);

        Path current = Paths.get(tree.getSourceFile().getName());
        builder.append("@source ").append(root.relativize(current).toString().replace('\\', '/')).append(":")
            .append(map.getLineNumber(position)).append("\n");
    }

    private static class EmittedMethod {
        private final MethodInfo method;
        private final String docComment;
        private final String signature;
        private boolean used = false;

        EmittedMethod(MethodInfo method, String docComment, String signature) {
            this.method = method;
            this.docComment = docComment;
            this.signature = signature;
        }

        boolean isUsed() {
            return used;
        }

        void emit(String prefix, StringBuilder builder) {
            used = true;

            builder.append(docComment);
            builder.append("function ").append(prefix).append(method.name()).append("(").append(signature).append(") end\n");
            for (String name : method.otherNames()) {
                builder.append(prefix).append(name).append(" = ").append(prefix).append(method.name()).append("\n");
            }
        }
    }

    private final class MethodCollection {
        private final ClassInfo info;
        private final Element enclosing;
        private final TypeMirror type;
        private final List<EmittedMethod> methods;

        private MethodCollection(ClassInfo info, Element enclosing, List<EmittedMethod> methods) {
            this.info = info;
            this.enclosing = enclosing;
            this.type = enclosing.asType();
            this.methods = methods;
        }

        boolean appearsIn(@Nonnull ClassInfo klass) {
            return info == klass || env.types().isAssignable(klass.element().asType(), type);
        }

        void emit(String prefix, StringBuilder builder) {
            for (EmittedMethod method : methods) {
                builder.append("\n");
                method.emit(prefix, builder);
            }
        }
    }
}
