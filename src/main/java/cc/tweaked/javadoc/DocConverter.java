/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package cc.tweaked.javadoc;

import com.sun.source.doctree.*;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SimpleDocTreeVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DocConverter extends SimpleDocTreeVisitor<Void, StringBuilder> {
    private final Element owner;
    private final Environment environment;
    private final DocTreePath path;
    private final Function<Element, String> resolve;

    private boolean inPre = false;

    private boolean hasParam = false;
    private final Map<String, List<? extends DocTree>> params = new HashMap<>();

    private boolean hasReturn = false;
    private List<? extends DocTree> returns;

    public DocConverter(Environment environment, Element owner, Function<Element, String> resolve) {
        this.owner = owner;
        this.environment = environment;

        DocCommentTree tree = environment.trees().getDocCommentTree(owner);
        this.path = tree == null ? null : new DocTreePath(environment.trees().getPath(owner), tree);
        this.resolve = resolve;
    }

    public boolean hasParam() {
        return hasParam;
    }

    @Nonnull
    public Map<String, List<? extends DocTree>> getParams() {
        return params;
    }

    public boolean hasReturn() {
        return hasReturn;
    }

    @Nullable
    public List<? extends DocTree> getReturns() {
        return returns;
    }

    @Override
    public Void visitDocComment(DocCommentTree node, StringBuilder stringBuilder) {
        visit(node.getFullBody(), stringBuilder);
        stringBuilder.append("\n");
        visit(node.getBlockTags(), stringBuilder);
        return null;
    }

    @Override
    public Void visitText(TextTree node, StringBuilder stringBuilder) {
        stringBuilder.append(node.getBody());
        return null;
    }

    @Override
    public Void visitLiteral(LiteralTree node, StringBuilder stringBuilder) {
        String body = node.getBody().getBody();

        if (inPre) {
            // This is a terrible hack, but one which (sadly) works. We skip emitting backticks if we're within a pre
            // block, as this will have inserted backticks already.
            // We also attempt to normalise code blocks here by trimming leading spaces (due to the "* ") and any
            // whitespace.
            body = body.strip();
            if (body.indexOf('\n') > 0) {
                body = Arrays.stream(body.split("\n"))
                    .map(x -> x.length() > 0 && x.charAt(0) == ' ' ? x.substring(1) : x)
                    .collect(Collectors.joining("\n"));
            }

            stringBuilder.append(body);
            return null;
        }

        switch (body) {
            case "nil":
            case "true":
            case "false":
                stringBuilder.append("@{").append(body).append("}");
                return null;
            default:
                stringBuilder.append("`").append(body).append("`");
                return null;
        }
    }

    @Override
    public Void visitLink(LinkTree node, StringBuilder stringBuilder) {
        stringBuilder.append("@{");
        visit(node.getReference(), stringBuilder);
        if (!node.getLabel().isEmpty()) {
            stringBuilder.append("|");
            visit(node.getLabel(), stringBuilder);
        }
        stringBuilder.append("}");
        return null;
    }

    @Override
    public Void visitReference(ReferenceTree node, StringBuilder stringBuilder) {
        Element referred = environment.trees().getElement(DocTreePath.getPath(path, node));
        if (referred == null) {
            environment.message(Diagnostic.Kind.ERROR, "Cannot resolve reference.", owner, node);
            stringBuilder.append(node.getSignature());
            return null;
        }

        String alternative = resolve.apply(referred);
        if (alternative == null) {
            environment.message(Diagnostic.Kind.ERROR, "Cannot convert " + referred + " into a Lua reference.", owner, node);
            stringBuilder.append(node.getSignature());
            return null;
        }

        stringBuilder.append(alternative);
        return null;
    }

    @Override
    public Void visitHidden(HiddenTree node, StringBuilder stringBuilder) {
        stringBuilder.append("@local\n");
        return null;
    }

    @Override
    public Void visitParam(ParamTree node, StringBuilder stringBuilder) {
        params.put(node.getName().getName().toString(), node.getDescription());
        return null;
    }

    @Override
    public Void visitReturn(ReturnTree node, StringBuilder stringBuilder) {
        returns = node.getDescription();
        return null;
    }

    @Override
    public Void visitThrows(ThrowsTree node, StringBuilder stringBuilder) {
        List<? extends DocTree> desc = node.getDescription();
        if (!desc.isEmpty() && desc.get(0).getKind() == DocTree.Kind.TEXT && ((TextTree) desc.get(0)).getBody().startsWith("(hidden)")) {
            return null;
        }

        stringBuilder.append("@throws ");
        visit(desc, stringBuilder);
        stringBuilder.append("\n");
        return null;
    }

    @Override
    public Void visitDeprecated(DeprecatedTree node, StringBuilder stringBuilder) {
        stringBuilder.append("@deprecated ");
        visit(node.getBody(), stringBuilder);
        stringBuilder.append("\n");
        return null;
    }

    @Override
    public Void visitUnknownBlockTag(UnknownBlockTagTree node, StringBuilder stringBuilder) {
        String name = node.getTagName();
        if (!name.startsWith("cc.")) return super.visitUnknownBlockTag(node, stringBuilder);

        String actualName = name.substring(3);
        switch (actualName) {
            case "param":
            case "tparam":
                hasParam = true;
                break;
            case "return":
            case "treturn":
                hasReturn = true;
                break;
            case "module":
                return null;
        }

        stringBuilder.append("@").append(actualName);

        List<? extends DocTree> children = node.getContent();
        if (children.isEmpty() || children.get(0).getKind() != DocTree.Kind.TEXT || !((TextTree) children.get(0)).getBody().startsWith("[")) {
            stringBuilder.append(" ");
        }

        visit(children, stringBuilder);
        stringBuilder.append("\n");
        return null;
    }

    @Override
    public Void visitSee(SeeTree node, StringBuilder stringBuilder) {
        List<? extends DocTree> ref = node.getReference();
        if (ref.isEmpty()) return null;

        stringBuilder.append("@see ");
        visit(ref.get(0), stringBuilder);
        stringBuilder.append(" ");
        visit(ref.subList(1, ref.size()), stringBuilder);
        stringBuilder.append("\n");
        return null;
    }

    @Override
    public Void visitStartElement(StartElementTree node, StringBuilder stringBuilder) {
        if (node.getName().contentEquals("pre")) {
            inPre = true;
            stringBuilder.append("```lua\n");
            return null;
        }

        stringBuilder.append("<").append(node.getName()).append(node.isSelfClosing() ? " />" : ">");
        return null;
    }

    @Override
    public Void visitEndElement(EndElementTree node, StringBuilder stringBuilder) {
        if (node.getName().contentEquals("pre")) {
            inPre = false;
            stringBuilder.append("\n```");
            return null;
        }

        stringBuilder.append("</").append(node.getName()).append(">");
        return null;
    }

    @Override
    protected Void defaultAction(DocTree node, StringBuilder stringBuilder) {
        report(node, "Visiting unknown node " + node.getKind());
        return null;
    }

    protected void report(DocTree node, String message) {
        DocTrees trees = environment.trees();
        environment.trees().printMessage(
            Diagnostic.Kind.ERROR, message, node,
            trees.getDocCommentTree(owner), trees.getPath(owner).getCompilationUnit()
        );
    }
}
