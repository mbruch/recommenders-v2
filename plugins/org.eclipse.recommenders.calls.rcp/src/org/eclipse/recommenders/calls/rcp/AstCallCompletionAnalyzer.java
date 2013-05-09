package org.eclipse.recommenders.calls.rcp;

import static com.google.common.base.Optional.*;
import static org.eclipse.recommenders.utils.rcp.JdtUtils.findFirstDeclaration;

import java.util.List;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;
import org.eclipse.recommenders.calls.ICallModel.DefinitionType;
import org.eclipse.recommenders.completion.rcp.IRecommendersCompletionContext;
import org.eclipse.recommenders.utils.names.IMethodName;
import org.eclipse.recommenders.utils.rcp.ast.ASTNodeUtils;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class AstCallCompletionAnalyzer {

    private final IRecommendersCompletionContext ctx;
    private String receiverName;

    private Optional<IType> receiverType;
    private Optional<IMethod> overrides;
    private DefinitionType defType;
    private List<IMethodName> calls;
    private IMethodName definedBy;

    public AstCallCompletionAnalyzer(IRecommendersCompletionContext context) {
        this.ctx = context;
    }

    public Optional<IType> getReceiverType() {
        if (receiverType == null) {
            findReceiver();
        }
        return receiverType;
    }

    private void findReceiver() {
        receiverName = ctx.getReceiverName();
        receiverType = ctx.getReceiverType();
        if (isReceiverNameThis() || isReceiverNameSuper() || isReceiverNameImplicitThis()) {
            // receiver may be this!
            receiverName = "super";
            setReceiverToSupertype();
        }
    }

    private boolean isReceiverNameThis() {
        return "this".equals(receiverName);
    }

    private boolean isReceiverNameSuper() {
        return "super".equals(receiverName);
    }

    private boolean isReceiverNameImplicitThis() {
        return "".equals(receiverName);
    }

    private void setReceiverToSupertype() {
        try {
            final IMethod m = ctx.getEnclosingMethod().orNull();
            if (m == null || JdtFlags.isStatic(m)) {
                return;
            }
            final IType type = m.getDeclaringType();
            final ITypeHierarchy hierarchy = SuperTypeHierarchyCache.getTypeHierarchy(type);
            receiverType = fromNullable(hierarchy.getSuperclass(type));
        } catch (final Exception e) {
            // RecommendersPlugin.logError(e, "Failed to resolve super type of %s", ctx.getEnclosingElement());
        }
    }

    public Optional<IMethod> getOverridesContext() {
        if (overrides == null) {
            findOverridesContext();
        }
        return overrides;
    }

    private void findOverridesContext() {
        overrides = ctx.getEnclosingMethod();
        if (overrides.isPresent()) {
            IMethod root = findFirstDeclaration(overrides.get());
            overrides = of(root);
        }
    }

    public DefinitionType getReceiverDefinitionType() {
        if (defType == null) {
            findCalls();
        }
        return defType;
    }

    private void findCalls() {
        calls = Lists.newLinkedList();
        IMethod jdtMethod = ctx.getEnclosingMethod().orNull();
        if (jdtMethod == null) {
            return;
        }
        CompilationUnit ast = ctx.getAST();
        MethodDeclaration astMethod = ASTNodeUtils.find(ast, jdtMethod).orNull();
        if (astMethod == null) {
            return;
        }

        final AstDefUseFinder r = new AstDefUseFinder(receiverName, astMethod);
        calls = r.getCalls();
        defType = r.getDefinitionType();
        definedBy = r.getDefiningMethod();
    }

    public List<IMethodName> getCalls() {
        if (calls == null) {
            findCalls();
        }
        return calls;
    }

    public Optional<IMethodName> getDefinedBy() {
        return fromNullable(definedBy);
    }
}
