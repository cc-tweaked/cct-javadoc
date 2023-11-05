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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.function.Function;

public class DocConverter extends SimpleDocTreeVisitor<Void, StringBuilder> {
    private final Element owner;
    private final Environment environment;
    private final DocTreePath path;
    private final Function<Element, String> resolve;

    private boolean inPre;
    private final Deque<String> indents = new ArrayDeque<>();
    private String indent = "";

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
        emitText(node.getBody(), stringBuilder, false);
        return null;
    }

    @Override
    public Void visitErroneous(ErroneousTree node, StringBuilder stringBuilder) {
        emitText(node.getBody(), stringBuilder, false);
        return null;
    }

    private void emitText(String body, StringBuilder builder, boolean stripFirst) {
        if (body.indexOf('\n') < 0) {
            builder.append(body);
        } else {
            String[] lines = body.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if ((i > 0 || stripFirst) && line.startsWith(" ")) line = line.substring(1);

                if (i > 0) {
                    builder.append("\n");
                    if (line.length() > 0) builder.append(indent);
                }

                builder.append(line);
            }
        }
    }

    @Override
    public Void visitLiteral(LiteralTree node, StringBuilder stringBuilder) {
        String body = node.getBody().getBody();

        if (inPre) {
            // This is a terrible hack, but one which (sadly) works. We skip emitting backticks if we're within a pre
            // block, as this will have inserted backticks already.
            // We also attempt to normalise code blocks here by trimming leading spaces (due to the "* ") and any
            // whitespace.
            emitText(body.strip(), stringBuilder, true);
            return null;
        }

        switch (body) {
            case "nil":
            case "true":
            case "false":
                stringBuilder.append("[`").append(body).append("`]");
                return null;
            default:
                if (node.getKind() == DocTree.Kind.CODE) {
                    stringBuilder.append("`").append(body).append("`");
                } else {
                    stringBuilder.append(body);
                }
                return null;
        }
    }

    @Override
    public Void visitLink(LinkTree node, StringBuilder stringBuilder) {
        if (!node.getLabel().isEmpty()) {
            stringBuilder.append("[");
            visit(node.getLabel(), stringBuilder);
            stringBuilder.append("]");
        }
        stringBuilder.append("[`");
        visit(node.getReference(), stringBuilder);
        stringBuilder.append("`]");
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
            stringBuilder.append("```lua");
            if (!node.getAttributes().isEmpty()) {
                stringBuilder.append(" {");
                boolean first = true;
                for (var attribute : node.getAttributes()) {
                    if (first) {
                        first = false;
                    } else {
                        stringBuilder.append(' ');
                    }
                    var attr = (AttributeTree) attribute;
                    stringBuilder.append(attr.getName()).append('=').append(attr.getValue());
                }
                stringBuilder.append('}');
            }
            stringBuilder.append('\n');
        } else if (node.getName().contentEquals("p")) {
            // No-op
        } else if (node.getName().contentEquals("ul")) {
            // No-op
        } else if (node.getName().contentEquals("li")) {
            indents.push(indent);
            indent += "   ";
            stringBuilder.append(" - ");
        } else if (node.getName().contentEquals("em") && node.getAttributes().isEmpty()) {
            stringBuilder.append("*");
        } else if (node.getName().contentEquals("strong") && node.getAttributes().isEmpty()) {
            stringBuilder.append("**");
        } else {
            stringBuilder.append("<").append(node.getName());
            visit(node.getAttributes(), stringBuilder);
            stringBuilder.append(node.isSelfClosing() ? " />" : ">");
        }
        return null;
    }

    @Override
    public Void visitAttribute(AttributeTree node, StringBuilder stringBuilder) {
        stringBuilder.append(' ').append(node.getName());
        switch (node.getValueKind()) {
            default:
            case EMPTY:
                return null;
            case DOUBLE:
                stringBuilder.append("=\"").append(node.getValue()).append('"');
                return null;
            case SINGLE:
                stringBuilder.append("='").append(node.getValue()).append('\'');
                return null;
            case UNQUOTED:
                stringBuilder.append('=').append(node.getValue());
                return null;
        }
    }

    @Override
    public Void visitEndElement(EndElementTree node, StringBuilder stringBuilder) {
        if (node.getName().contentEquals("pre")) {
            inPre = false;
            stringBuilder.append(indent).append("\n```");
        } else if (node.getName().contentEquals("ul")) {
            // No-op
        } else if (node.getName().contentEquals("li")) {
            indent = indents.pop();
        } else if (node.getName().contentEquals("em")) {
            stringBuilder.append("*");
        } else if (node.getName().contentEquals("strong")) {
            stringBuilder.append("**");
        } else {
            stringBuilder.append("</").append(node.getName()).append(">");
        }
        return null;
    }

    @Override
    public Void visitEntity(EntityTree node, StringBuilder stringBuilder) {
        stringBuilder.append("&").append(node.getName()).append(";");
        return null;
    }

    @Override
    public Void visitInheritDoc(InheritDocTree node, StringBuilder out) {
        DocCommentTree tree = null;
        if (owner instanceof ExecutableElement currentMethod) {
            Queue<TypeElement> types = new ArrayDeque<>();
            TypeElement currentType = (TypeElement) currentMethod.getEnclosingElement();
            addSupers(types, currentType);

            TypeElement superType;
            top:
            while ((superType = types.poll()) != null) {
                for (ExecutableElement superMethod : ElementFilter.methodsIn(superType.getEnclosedElements())) {
                    if (environment.elements().overrides(currentMethod, superMethod, currentType)) {
                        tree = environment.trees().getDocCommentTree(superMethod);
                        if (tree != null) break top;
                    }
                }

                addSupers(types, superType);
            }
        }

        if (tree == null) {
            report(node, "Cannot resolve parent doc comment.");
            return null;
        } else {
            return visit(tree, out);
        }
    }

    private static void addSupers(Queue<TypeElement> queue, TypeElement element) {
        if (element.getSuperclass().getKind() == TypeKind.DECLARED) {
            queue.add((TypeElement) ((DeclaredType) element.getSuperclass()).asElement());
        }

        for (TypeMirror iface : element.getInterfaces()) {
            if (iface.getKind() == TypeKind.DECLARED) queue.add((TypeElement) ((DeclaredType) iface).asElement());
        }
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

    public void message(@Nonnull Diagnostic.Kind kind, @Nonnull String message, @Nonnull Element element) {
        if (path != null) environment.message(kind, message, element);
    }
}
