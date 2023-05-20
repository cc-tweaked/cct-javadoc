/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package cc.tweaked.javadoc;

import com.google.auto.common.MoreTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;

public final class Helpers {
    private Helpers() {
    }

    @Nullable
    public static AnnotationMirror getAnnotation(@Nonnull AnnotatedConstruct element, @Nonnull TypeElement type) {
        return element.getAnnotationMirrors()
            .stream()
            .filter(x -> x.getAnnotationType().asElement() == type)
            .findAny().orElse(null);
    }

    @Nullable
    public static Object getAnnotationValue(@Nonnull AnnotationMirror element, @Nonnull String name) {
        return element.getElementValues().entrySet().stream()
            .filter(x -> x.getKey().getSimpleName().contentEquals(name))
            .map(x -> x.getValue().getValue())
            .findAny().orElse(null);
    }

    @Nullable
    public static TypeMirror unwrapOptional(TypeMirror type) {
        return is(type, Optional.class) ? ((DeclaredType) type).getTypeArguments().get(0) : null;
    }

    public static TypeMirror unwrapCoerced(TypeMirror type) {
        return is(type, "dan200.computercraft.api.lua.Coerced") ? ((DeclaredType) type).getTypeArguments().get(0) : type;
    }


    public static boolean is(TypeMirror type, Class<?> klass) {
        return type.getKind() == TypeKind.DECLARED && MoreTypes.isTypeOf(klass, type);
    }

    public static boolean is(TypeMirror type, String name) {
        return type.getKind() == TypeKind.DECLARED && is(MoreTypes.asTypeElement(type), name);
    }

    public static boolean is(TypeElement type, Class<?> klass) {
        return type.getQualifiedName().contentEquals(klass.getCanonicalName());
    }

    public static boolean is(TypeElement type, String name) {
        return type.getQualifiedName().contentEquals(name);
    }

    public static boolean isAny(TypeMirror type) {
        return is(type, "dan200.computercraft.api.lua.MethodResult")
            || is(type, "dan200.computercraft.api.lua.IArguments")
            || is(type, "dan200.computercraft.api.lua.Arguments")
            || (type.getKind() == TypeKind.ARRAY && is(((ArrayType) type).getComponentType(), Object.class));
    }

    public static boolean isKnown(TypeMirror type) {
        type = Optional.ofNullable(unwrapOptional(type)).orElse(type);
        type = unwrapCoerced(type);

        switch (type.getKind()) {
            case DOUBLE:
            case INT:
            case LONG:
            case BOOLEAN:
                return true;
            case DECLARED:
                return is(type, String.class);
            default:
                return false;
        }
    }

    public static boolean isIrrelevant(TypeMirror type) {
        return is(type, "dan200.computercraft.api.lua.ILuaContext")
            || is(type, "dan200.computercraft.api.lua.LuaContext")
            || is(type, "dan200.computercraft.api.peripheral.IComputerAccess")
            || is(type, "dan200.computercraft.api.peripheral.ComputerAccess");
    }
}
