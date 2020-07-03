/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package cc.tweaked.javadoc;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.UnknownBlockTagTree;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Optional;

/**
 * Information about a class.
 */
public class ClassInfo {
    private final String name;
    private final Kind kind;
    private final TypeElement element;
    private final DocCommentTree doc;
    private final boolean hidden;
    private final String moduleName;
    private final String typeName;

    private ClassInfo(@Nonnull String module, @Nonnull Kind kind, @Nonnull TypeElement element, @Nonnull DocCommentTree doc, boolean hidden) {
        this.name = module;
        this.kind = kind;
        this.element = element;
        this.doc = doc;
        this.hidden = hidden;

        if (kind != Kind.TYPE) {
            moduleName = name;
            typeName = null;
        } else {
            int index = name.indexOf('.');
            moduleName = index < 0 ? name : name.substring(0, index);
            typeName = index < 0 ? name : name.substring(index + 1);
        }
    }

    /**
     * Attempt to construct a {@link ClassInfo}.
     *
     * @param env  The environment to construct within.
     * @param type The method we're wrapping.
     * @return Information about this method, if available.
     */
    @Nonnull
    public static Optional<ClassInfo> of(@Nonnull Environment env, @Nonnull TypeElement type) {
        DocCommentTree doc = env.trees().getDocCommentTree(type);
        if (doc == null) return Optional.empty();

        String name = doc.getBlockTags().stream()
            .filter(UnknownBlockTagTree.class::isInstance).map(UnknownBlockTagTree.class::cast)
            .filter(x -> x.getTagName().equals("cc.module"))
            .map(UnknownBlockTagTree::getContent)
            .findAny().map(ClassInfo::getName).orElse(null);

        boolean hidden = doc.getBlockTags().stream().anyMatch(x -> x.getKind() == DocTree.Kind.HIDDEN);

        if (name == null || name.isEmpty()) return Optional.empty();

        Kind kind
            = env.types().isAssignable(type.asType(), env.getLuaApiType()) ? Kind.API
            : env.types().isAssignable(type.asType(), env.getPeripheralType()) ? Kind.PERIPHERAL
            : Kind.TYPE;

        return Optional.of(new ClassInfo(name, kind, type, doc, hidden));
    }

    private static String getName(List<? extends DocTree> tree) {
        if (tree == null) return null;
        StringBuilder builder = new StringBuilder();
        for (DocTree child : tree) {
            if (child.getKind() == DocTree.Kind.TEXT) builder.append(((TextTree) child).getBody());
        }
        return builder.toString();
    }

    public enum Kind {
        API,
        PERIPHERAL,
        TYPE
    }

    @Nonnull
    public String moduleName() {
        return moduleName;
    }

    @Nullable
    public String typeName() {
        return typeName;
    }

    @Nonnull
    public String name() {
        return name;
    }

    @Nonnull
    public Kind kind() {
        return kind;
    }

    @Nonnull
    public TypeElement element() {
        return element;
    }

    @Nonnull
    public DocCommentTree doc() {
        return doc;
    }

    public boolean isHidden() {
        return hidden;
    }
}
