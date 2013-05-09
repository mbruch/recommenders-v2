/**
 * Copyright (c) 2010, 2013 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.calls.rcp;

import static org.eclipse.recommenders.utils.Checks.*;
import static org.eclipse.recommenders.utils.rcp.ast.BindingUtils.*;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.recommenders.calls.ICallModel.DefinitionType;
import org.eclipse.recommenders.utils.annotations.Nullable;
import org.eclipse.recommenders.utils.names.IMethodName;
import org.eclipse.recommenders.utils.rcp.ast.BindingUtils;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class AstDefUseFinder extends ASTVisitor {

    private IMethodName definingMethod;
    private DefinitionType definitionType;
    private final List<IMethodName> calls = Lists.newLinkedList();
    private final MethodDeclaration method;
    private final String varname;

    public List<IMethodName> getCalls() {
        return calls;
    }

    public IMethodName getDefiningMethod() {
        return definingMethod;
    }

    public DefinitionType getDefinitionType() {
        return definitionType;
    }

    public AstDefUseFinder(final String varname, MethodDeclaration method) {
        this.varname = ensureIsNotNull(varname);
        this.method = ensureIsNotNull(method);
        method.accept(this);
    }

    @Override
    public boolean visit(final MethodInvocation node) {
        Expression expr = node.getExpression();
        if (receiverExpressionMatchesVarname(expr) || isThis() && isReceiverThis(node)) {
            final IMethodBinding b = node.resolveMethodBinding();
            registerMethodCallOnReceiver(b);
        }
        return true;
    }

    private boolean receiverExpressionMatchesVarname(@Nullable final Expression exp) {
        if (exp == null) {
            return false;
        }
        switch (exp.getNodeType()) {
        case ASTNode.SIMPLE_NAME:
        case ASTNode.QUALIFIED_NAME:
            final Name name = cast(exp);
            // is it the same name we are looking for?
            return matchesVarName(name);
        case ASTNode.THIS_EXPRESSION:
            // do we look for this?
            return isThis();
        default:
            return false;
        }
    }

    private boolean matchesVarName(@Nullable final Name node) {
        if (node == null) {
            return false;
        }
        final String name = node.getFullyQualifiedName();
        return varname.equals(name);
    }

    private boolean isThis() {
        return "this".equals(varname) || "".equals(varname);
    }

    private boolean isReceiverThis(final MethodInvocation mi) {
        final Expression expression = mi.getExpression();
        // standard case:
        if (expression == null && !isStatic(mi)) {
            return true;
        }
        // qualified call: this.method()
        if (expression instanceof ThisExpression) {
            return true;
        }
        return false;
    }

    private boolean isStatic(final MethodInvocation call) {
        final IMethodBinding binding = call.resolveMethodBinding();
        if (binding != null) {
            return JdtFlags.isStatic(binding);
        }
        // let's assume it's not static...
        return false;
    }

    private void registerMethodCallOnReceiver(final IMethodBinding b) {
        final Optional<IMethodName> opt = BindingUtils.toMethodName(b);
        if (opt.isPresent()) {
            calls.add(opt.get());
        }
    }

    @Override
    public boolean visit(AnonymousClassDeclaration node) {
        return false;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        return false;
    }

    @Override
    public boolean visit(final Assignment node) {

        final Expression lhs = node.getLeftHandSide();
        if (lhs == null) {
            return true;
        }
        switch (lhs.getNodeType()) {
        case ASTNode.SIMPLE_NAME:
        case ASTNode.QUALIFIED_NAME:
            final Name n = cast(lhs);
            if (!matchesVarName(n)) {
                return true;
            }
            break;
        default:
            return true;
        }
        final Expression rhs = node.getRightHandSide();
        evaluateRightHandSideExpression(rhs);
        return true;
    }

    // called only if left hand side was a match.
    private void evaluateRightHandSideExpression(final Expression expression) {
        switch (expression.getNodeType()) {
        case ASTNode.CAST_EXPRESSION:
            final CastExpression ce = cast(expression);
            // re-evaluate using the next expression:
            evaluateRightHandSideExpression(ce.getExpression());
            break;
        case ASTNode.METHOD_INVOCATION:
            // x = some().method().call()
            final MethodInvocation mi = cast(expression);
            definingMethod = toMethodName(mi.resolveMethodBinding()).orNull();
            definitionType = DefinitionType.METHOD_RETURN;
            break;
        case ASTNode.SUPER_METHOD_INVOCATION:
            // x = super.some()
            final SuperMethodInvocation smi = cast(expression);
            definingMethod = toMethodName(smi.resolveMethodBinding()).orNull();
            definitionType = DefinitionType.METHOD_RETURN;
            break;
        case ASTNode.CLASS_INSTANCE_CREATION:
            final ClassInstanceCreation cic = cast(expression);
            definingMethod = toMethodName(cic.resolveConstructorBinding()).orNull();
            definitionType = DefinitionType.NEW;
            break;
        case ASTNode.SIMPLE_NAME:
            // e.g. int j=anotherValue;
            // some alias thing...
            // it might be that we found an assignment before and this simpleName is just "$missing". Then ignore this
            if (definitionType == null) {
                definitionType = DefinitionType.LOCAL;
            }
            break;
        default:
            break;
        }
    }

    // calls like 'this(args)'
    @Override
    public boolean visit(final ConstructorInvocation node) {
        if (isThis()) {
            final IMethodBinding b = node.resolveConstructorBinding();
            registerMethodCallOnReceiver(b);
        }
        return true;
    }

    @Override
    public boolean visit(final SingleVariableDeclaration node) {
        return true;
    }

    @Override
    public boolean visit(final SuperConstructorInvocation node) {
        if (isThis()) {
            final IMethodBinding b = node.resolveConstructorBinding();
            registerMethodCallOnReceiver(b);
        }
        return true;
    }

    @Override
    public boolean visit(final SuperMethodInvocation node) {
        if (isThis()) {
            final IMethodBinding b = node.resolveMethodBinding();
            registerMethodCallOnReceiver(b);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(final VariableDeclarationStatement node) {
        for (final VariableDeclarationFragment f : (List<VariableDeclarationFragment>) node.fragments()) {
            evaluateVariableDeclarationFragment(f);
        }
        return true;
    }

    private void evaluateVariableDeclarationFragment(final VariableDeclarationFragment f) {
        final SimpleName name = f.getName();
        if (matchesVarName(name)) {
            final Expression expression = f.getInitializer();
            if (expression != null) {
                evaluateRightHandSideExpression(expression);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(VariableDeclarationExpression node) {
        for (VariableDeclarationFragment f : (List<VariableDeclarationFragment>) node.fragments()) {
            evaluateVariableDeclarationFragment(f);
        }
        return true;
    }

}
