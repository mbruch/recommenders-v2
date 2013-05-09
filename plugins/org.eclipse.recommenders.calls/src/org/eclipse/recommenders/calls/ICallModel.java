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
package org.eclipse.recommenders.calls;

import java.util.List;
import java.util.Set;

import org.eclipse.recommenders.utils.Recommendation;
import org.eclipse.recommenders.utils.RecommendationsProcessor;
import org.eclipse.recommenders.utils.names.IMethodName;
import org.eclipse.recommenders.utils.names.ITypeName;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

/**
 * Thin layer around a Bayesian network designed for recommending method calls, definitions, and unordered object usage
 * patterns (list of calls).
 * <p>
 * Note that {@link ICallModel}s are stateful and thus should not be shared between - and used by - several recommenders
 * at the same time.
 */
@Beta
public interface ICallModel {

    /**
     * Returns the type this net makes recommendations for.
     */
    ITypeName getType();

    /**
     * Clears all observations and puts the network in its initial state.
     */
    void reset();

    /**
     * Adds a new observed call to the list of already observed calls. If the given method does not exist in this net it
     * is ignored.
     * 
     * @return returns <code>true</code> when a matching method node was found and put into observed state
     */
    boolean setObservedCall(final IMethodName additionalCalledMethod);

    /**
     * Sets the observed state for the given called methods. Note that information about previously called methods is
     * kept.
     * 
     * @return returns <code>true</code> when for <b>all</b> given methods a matching method node was found and put into
     *         observed state
     * @see #setObservedCall(IMethodName)
     */
    boolean setObservedCalls(final Set<IMethodName> additionalCalledMethods);

    /**
     * Sets the overridden method context. This is used to refine recommendations based on where, i.e., in the context
     * of which overridden method, a variable is used. Usually this is the name of the topmost (first) declaration
     * method of the enclosing method.
     * 
     * @return returns <code>true</code> when the given overridden method context is known
     */
    boolean setObservedOverrideContext(final IMethodName newOverriddenMethod);

    /**
     * Sets the information how the variable was initially defined.
     * <p>
     * Depending on the definition kind, the method specified here will be
     * <ul>
     * <li>{@link DefinitionType#METHOD_RETURN}: the method whose return value defined the variable,
     * <li>{@link DefinitionType#PARAMETER}: the method this variable was defined as a parameter for,
     * <li>{@link DefinitionType#NEW}: the constructor this variable was initialized with.
     * </ul>
     * 
     * @return returns <code>true</code> if the defining method was found, <code>false</code> otherwise
     */
    boolean setObservedDefiningMethod(final IMethodName newDefinedBy);

    /**
     * @see #setObservedDefiningMethod(IMethodName)
     */
    // TODO should setObservedDefinitionType and setObservedDefiningMethod be merged into a single method?
    boolean setObservedDefinitionType(final DefinitionType newDefType);

    /**
     * Sets the given pattern as observed. This call is ignored when the given name is not known.
     * 
     * @return returns <code>true</code> if the pattern was found, <code>false</code> otherwise
     */
    boolean setObservedPattern(final String newPatternName);

    /**
     * Returns the currently observed overridden method - if any. If this value is present, it usually points to the
     * topmost (first) declaration of the overridden method.
     */
    Optional<IMethodName> getObservedOverrideContext();

    /**
     * Returns the currently observed method that defined the variable under recommendation - if any.
     */
    Optional<IMethodName> getObservedDefiningMethod();

    /**
     * Returns the observed variable definition type - if any.
     */
    Optional<DefinitionType> getObservedDefinitionType();

    /**
     * Returns a list of observed methods flagged as being observed.
     */
    ImmutableSet<IMethodName> getObservedCalls();

    /**
     * Returns the list of all known callable methods that can be observed.
     * 
     * @see #setObservedCall(IMethodName)
     */
    ImmutableSet<IMethodName> getKnownCalls();

    /**
     * Returns the list of all known enclosing methods that can be observed.
     * 
     * @see #setObservedOverrideContext(IMethodName)
     */
    ImmutableSet<IMethodName> getKnownOverrideContexts();

    /**
     * Returns the list of all known patterns names that can be observed.
     * 
     * @see #setObservedPattern(String)
     */
    ImmutableSet<String> getKnownPatterns();

    /**
     * Returns a sorted set of recommended variable definitions.
     */
    List<Recommendation<IMethodName>> getRecommendedDefinitions(RecommendationsProcessor<IMethodName> processor);

    /**
     * Returns a sorted set of recommended usage patterns.
     */
    List<Recommendation<String>> getRecommendedPatterns(RecommendationsProcessor<String> processor);

    /**
     * Returns a sorted set of recommended method calls.
     */
    List<Recommendation<IMethodName>> getRecommendedCalls(RecommendationsProcessor<IMethodName> processor);

    /**
     * Specifies how the variable under examination was defined (field, parameter, by method return...).
     */
    public enum DefinitionType {
        /**
         * indicates that the variable was defined by a method return value, e.g, int x = p.getX();
         */
        METHOD_RETURN,
        /**
         * indicates that the variable was defined by a constructor call, e.g, Point p = new Point(x,y);
         */
        NEW,
        /**
         * indicates that the variable was defined by a field declaration
         */
        FIELD,
        /**
         * indicates that the variable was declared as a parameter of the enclosing method.
         */
        PARAMETER,
        /**
         * indicates that the variable represents "this"
         */
        THIS,
        /**
         * indicates that the variable was defined as a local variable. Usually this value gets replaced by more
         * specific values {@link #NEW}, or {@link #METHOD_RETURN} if possible.
         */
        LOCAL,
        /**
         * indicates that the variable was defined in an unexpected or unsupported and yet unhandled way.
         */
        UNKNOWN,
    }
}
