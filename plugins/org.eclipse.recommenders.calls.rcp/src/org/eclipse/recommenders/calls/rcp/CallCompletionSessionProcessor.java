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

import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.Math.rint;
import static org.eclipse.recommenders.calls.rcp.Constants.*;
import static org.eclipse.recommenders.internal.completion.rcp.ProcessableCompletionProposalComputer.NULL_PROPOSAL;
import static org.eclipse.recommenders.utils.Recommendations.top;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnMemberAccess;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnMessageSend;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnQualifiedNameReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnSingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.recommenders.calls.ICallModel;
import org.eclipse.recommenders.calls.ICallModelProvider;
import org.eclipse.recommenders.calls.NullCallModel;
import org.eclipse.recommenders.completion.rcp.IProcessableProposal;
import org.eclipse.recommenders.completion.rcp.IRecommendersCompletionContext;
import org.eclipse.recommenders.completion.rcp.SessionProcessor;
import org.eclipse.recommenders.internal.completion.rcp.SimpleProposalProcessor;
import org.eclipse.recommenders.models.BasedTypeName;
import org.eclipse.recommenders.models.ProjectCoordinate;
import org.eclipse.recommenders.models.rcp.ProjectCoordinateProvider;
import org.eclipse.recommenders.utils.Recommendation;
import org.eclipse.recommenders.utils.Recommendations;
import org.eclipse.recommenders.utils.names.IMethodName;
import org.eclipse.recommenders.utils.names.ITypeName;
import org.eclipse.recommenders.utils.rcp.JavaElementResolver;

import com.google.common.collect.Lists;

public class CallCompletionSessionProcessor extends SessionProcessor {

    @SuppressWarnings("serial")
    private final Set<Class<?>> supportedCompletionRequests = new HashSet<Class<?>>() {
        {
            add(CompletionOnMemberAccess.class);
            add(CompletionOnMessageSend.class);
            add(CompletionOnQualifiedNameReference.class);
            add(CompletionOnSingleNameReference.class);
        }
    };

    @Inject
    @Preference(P_MAX_NUMBER_OF_PROPOSALS)
    private int maxNumberOfProposals;

    @Inject
    @Preference(P_MIN_PROPOSAL_PROBABILITY)
    private int minProposalProbability;

    @Inject
    @Preference(P_UPDATE_PROPOSAL_RELEVANCE)
    private boolean updateProposalRelevance;

    @Inject
    @Preference(P_DECORATE_PROPOSAL_TEXT)
    private boolean decorateProposalText;

    private final ICallModelProvider modelProvider;
    private final ProjectCoordinateProvider projectCoordinateProvider;
    private final JavaElementResolver jdtResolver;

    private IRecommendersCompletionContext ctx;

    private AstCallCompletionAnalyzer completionAnalyzer;
    private ITypeName receiverTypeName;
    private ProjectCoordinate projectCoordinate;
    private ICallModel model;

    private Iterable<Recommendation<IMethodName>> recommendations;

    @Inject
    public CallCompletionSessionProcessor(final ProjectCoordinateProvider projectCoordinateProvider,
            final ICallModelProvider modelProvider, final JavaElementResolver jdtResolver) {
        this.jdtResolver = jdtResolver;
        this.projectCoordinateProvider = projectCoordinateProvider;
        this.modelProvider = modelProvider;
    }

    @Override
    public void startSession(final IRecommendersCompletionContext context) {
        ctx = context;
        recommendations = Lists.newLinkedList();
        completionAnalyzer = new AstCallCompletionAnalyzer(context);
        try {
            if (!isCompletionRequestSupported() || //
                    !findReceiverTypeAndModel() || //
                    !findRecommendations()) {
                return;
            }
        } finally {
            releaseModel();
        }
    }

    private boolean findReceiverTypeAndModel() {
        IType receiverType = completionAnalyzer.getReceiverType().orNull();
        if (receiverType == null) {
            return false;
        }
        projectCoordinate = projectCoordinateProvider.resolve(receiverType).orNull();
        if (projectCoordinate == null) {
            return false;
        }
        // TODO loop until we find a model. later
        receiverTypeName = jdtResolver.toRecType(receiverType);
        BasedTypeName name = new BasedTypeName(projectCoordinate, receiverTypeName);
        // TODO for testing
        model = modelProvider.acquireModel(name).or(NullCallModel.NULL_MODEL);
        return model != null;

    }

    private boolean isCompletionRequestSupported() {
        final ASTNode node = ctx.getCompletionNode().orNull();
        return node == null ? false : supportedCompletionRequests.contains(node.getClass());
    }

    private boolean findRecommendations() {
        model.reset();

        // set override-context:
        IMethod overrides = completionAnalyzer.getOverridesContext().orNull();
        if (overrides != null) {
            IMethodName crOverrides = jdtResolver.toRecMethod(overrides).or(
                    org.eclipse.recommenders.utils.Constants.UNKNOWN_METHOD);
            model.setObservedOverrideContext(crOverrides);
        }

        // set definition-type and defined-by
        model.setObservedDefinitionType(completionAnalyzer.getReceiverDefinitionType());
        IMethodName definedBy = completionAnalyzer.getDefinedBy().orNull();
        if (definedBy != null) {
            model.setObservedDefiningMethod(definedBy);
        }

        // set calls:
        model.setObservedCalls(newHashSet(completionAnalyzer.getCalls()));

        // read
        recommendations = model.getRecommendedCalls();
        // filter void methods if needed:
        if (ctx.getExpectedTypeSignature().isPresent()) {
            recommendations = Recommendations.filterVoid(recommendations);
        }
        top(recommendations, maxNumberOfProposals, minProposalProbability);
        return isEmpty(recommendations);
    }

    private void releaseModel() {
        if (model != null) {
            modelProvider.releaseModel(model);
            model = null;
        }
    }

    @Override
    public void process(final IProcessableProposal proposal) {
        if (isEmpty(recommendations)) {
            return;
        }

        final CompletionProposal coreProposal = proposal.getCoreProposal().or(NULL_PROPOSAL);
        switch (coreProposal.getKind()) {
        case CompletionProposal.METHOD_REF:
        case CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER:
        case CompletionProposal.METHOD_NAME_REFERENCE:
            final ProposalMatcher matcher = new ProposalMatcher(coreProposal);
            for (final Recommendation<IMethodName> call : recommendations) {
                final IMethodName crMethod = call.getProposal();
                if (!matcher.match(crMethod)) {
                    continue;
                }
                int percentage = (int) rint(call.getRelevance() * 100);

                int increment = 0;
                if (updateProposalRelevance) {
                    increment = 200 + percentage;
                }
                String label = "";
                if (decorateProposalText) {
                    label = percentage + " %";
                }
                if (updateProposalRelevance || decorateProposalText) {
                    proposal.getProposalProcessorManager().addProcessor(new SimpleProposalProcessor(increment, label));
                }
                // we found the proposal we are looking for. So quit.
                break;
            }
        }
    }
}
