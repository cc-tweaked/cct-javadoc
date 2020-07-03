/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package cc.tweaked.javadoc;

import com.sun.source.doctree.DocCommentTree;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Information about a method annotated with {@code @LuaFunction}.
 */
public class MethodInfo
{
    private final List<String> allNames;
    private final ExecutableElement method;
    private final DocCommentTree doc;

    public MethodInfo( @Nonnull List<String> allNames, @Nonnull ExecutableElement method, @Nullable DocCommentTree doc )
    {
        this.allNames = Collections.unmodifiableList( allNames );
        this.method = method;
        this.doc = doc;
    }

    /**
     * Attempt to construct a {@link MethodInfo}.
     *
     * @param env    The environment to construct within.
     * @param method The method we're wrapping.
     * @return Information about this method, if available.
     */
    @Nonnull
    public static Optional<MethodInfo> of( @Nonnull Environment env, @Nonnull ExecutableElement method )
    {
        // Only allow instance methods. Static methods are "generic peripheral" ones, and so are unsuitable.
        if( method.getModifiers().contains( Modifier.STATIC ) ) return Optional.empty();

        AnnotationMirror mirror = Helpers.getAnnotation( method, env.getLuaFunction() );
        if( mirror == null ) return Optional.empty();

        @SuppressWarnings( { "unchecked", "rawtypes" } )
        List<AnnotationValue> overrideNames = (List) Helpers.getAnnotationValue( mirror, "value" );

        return Optional.of( new MethodInfo(
            overrideNames == null
                ? List.of( method.getSimpleName().toString() )
                : overrideNames.stream().map( x -> (String) x.getValue() ).collect( Collectors.toList() ),
            method, env.trees().getDocCommentTree( method )
        ) );
    }

    @Nonnull
    public String name()
    {
        return allNames.get( 0 );
    }

    @Nonnull
    public List<String> otherNames()
    {
        return allNames.subList( 1, allNames.size() );
    }

    @Nonnull
    public ExecutableElement element()
    {
        return method;
    }

    @Nullable
    public DocCommentTree doc()
    {
        return doc;
    }
}
