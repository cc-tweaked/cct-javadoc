/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package cc.tweaked.javadoc;

import com.sun.source.doctree.DocTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class Environment {
    public static final String LUA_FUNCTION = "dan200.computercraft.api.lua.LuaFunction";
    public static final String PERIPHERAL = "dan200.computercraft.api.peripheral.IPeripheral";
    public static final String LUA_API = "dan200.computercraft.api.lua.ILuaAPI";
    public static final String GENERIC_PERIPHERAL = "dan200.computercraft.api.lua.GenericSource";

    private final DocletEnvironment env;
    private final Reporter reporter;

    private final TypeElement luaFunction;
    private final TypeMirror luaApiType;
    private final TypeMirror peripheralType;
    private final TypeMirror genericPeripheralType;

    private Environment(DocletEnvironment env, Reporter reporter) {
        this.env = env;
        this.reporter = reporter;

        Elements elements = env.getElementUtils();
        luaFunction = elements.getTypeElement(LUA_FUNCTION);
        luaApiType = asType(elements.getTypeElement(LUA_API));
        peripheralType = asType(elements.getTypeElement(PERIPHERAL));
        genericPeripheralType = asType(elements.getTypeElement(GENERIC_PERIPHERAL));
    }

    public static Environment of(DocletEnvironment environment, Reporter reporter) {
        Environment env = new Environment(environment, reporter);
        if (env.luaFunction == null) {
            env.message(Diagnostic.Kind.ERROR, "Cannot find @LuaFunction");
            return null;
        }

        if (env.luaApiType == null) {
            env.message(Diagnostic.Kind.ERROR, "Cannot find IAPI");
            return null;
        }

        if (env.peripheralType == null) {
            env.message(Diagnostic.Kind.ERROR, "Cannot find IPeripheral");
            return null;
        }

        return env;
    }

    @Nonnull
    public Elements elements() {
        return env.getElementUtils();
    }

    @Nonnull
    public Types types() {
        return env.getTypeUtils();
    }

    @Nonnull
    public DocTrees trees() {
        return env.getDocTrees();
    }

    public Reporter reporter() {
        return reporter;
    }

    public void message(@Nonnull Diagnostic.Kind kind, @Nonnull String message) {
        reporter.print(kind, message);
    }

    public void message(@Nonnull Diagnostic.Kind kind, @Nonnull String message, @Nonnull Element element) {
        reporter.print(kind, element, message);
    }

    public void message(@Nonnull Diagnostic.Kind kind, @Nonnull String message, @Nonnull Element element, @NonNull DocTree tree) {
        reporter.print(kind, DocTreePath.getPath(trees().getPath(element), trees().getDocCommentTree(element), tree), message);
    }

    @Nonnull
    public TypeElement getLuaFunction() {
        return luaFunction;
    }

    @Nonnull
    public TypeMirror getLuaApiType() {
        return luaApiType;
    }

    @Nonnull
    public TypeMirror getPeripheralType() {
        return peripheralType;
    }

    @Nonnull
    public TypeMirror getGenericPeripheralType() {
        return genericPeripheralType;
    }

    public void trying(@Nonnull Element element, Runnable function) {
        try {
            function.run();
        } catch (RuntimeException e) {
            StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                e.printStackTrace(out);
            }

            message(Diagnostic.Kind.ERROR, writer.toString(), element);
        }
    }

    private static TypeMirror asType(TypeElement element) {
        return element == null ? null : element.asType();
    }

    private interface MsgReporter {
        void message(@Nonnull Diagnostic.Kind kind, @Nonnull String message, @Nullable Element element);
    }
}
