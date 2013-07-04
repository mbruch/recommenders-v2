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

import static com.google.common.base.Optional.*;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ImmutableSet.copyOf;
import static org.eclipse.recommenders.utils.Constants.*;
import static org.eclipse.recommenders.utils.Recommendation.newRecommendation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.recommenders.commons.bayesnet.BayesianNetwork;
import org.eclipse.recommenders.commons.bayesnet.Node;
import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.junctionTree.JunctionTreeAlgorithm;
import org.eclipse.recommenders.utils.Constants;
import org.eclipse.recommenders.utils.Recommendation;
import org.eclipse.recommenders.utils.annotations.Nullable;
import org.eclipse.recommenders.utils.names.IFieldName;
import org.eclipse.recommenders.utils.names.IMethodName;
import org.eclipse.recommenders.utils.names.ITypeName;
import org.eclipse.recommenders.utils.names.VmMethodName;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Lists;

/**
 * A thin wrapper around a {@link BayesianNetwork} for recommending method calls.
 * <p>
 * The Bayesian network is expected to follow the structure specified below:
 * <ul>
 * <li>every node must have at least <b>2 states</b>!
 * <li>the first state is supposed to be a dummy state. Call it like {@link Constants#N_STATE_DUMMY_CTX}
 * <li>the second state <b>may</b> to be a dummy state too if no valuable other state could be found.
 * </ul>
 * <ul>
 * <li><b>callgroup node (formerly called pattern node):</b>
 * <ul>
 * <li>node name: {@link Constants#N_NODEID_CALL_GROUPS}
 * <li>state names: no constraints. Recommended schema is to use 'p'#someNumber.
 * </ul>
 * <li><b>context node:</b>
 * <ul>
 * <li>node name: {@link Constants#N_NODEID_CONTEXT}
 * <li>state names: fully-qualified method names as returned by {@link IMethodName#getIdentifier()}.
 * </ul>
 * <li><b>definition node:</b>
 * <ul>
 * <li>node name: {@link Constants#N_NODEID_DEF}
 * <li>state names: fully-qualified names as returned by {@link IMethodName#getIdentifier()} or
 * {@link IFieldName#getIdentifier()}.
 * </ul>
 * <li><b>definition kind node:</b>
 * <ul>
 * <li>node name: {@link Constants#N_NODEID_DEF_KIND}
 * <li>state names: one of {@link DefinitionType}, i.e., METHOD_RETURN, NEW, FIELD, PARAMETER, THIS, UNKNOWN, or ANY
 * </ul>
 * <li><b>method call node:</b>
 * <ul>
 * <li>node name: {@link IMethodName#getIdentifier()}
 * <li>state names: {@link Constants#N_STATE_TRUE} or {@link Constants#N_STATE_FALSE}
 * </ul>
 * </ul>
 */
@Beta
public class JayesCallModel implements ICallModel {

    private final class StringToMethodNameFunction implements Function<String, IMethodName> {
        @Override
        public IMethodName apply(final String input) {
            return VmMethodName.get(input);
        }
    }

    /**
     * IT's NULL
     */
    private static final String N_STATE_UNOBSERVED = null;

    private BayesNet net;
    private BayesNode callgroupNode;
    private BayesNode overridesNode;
    private BayesNode definedByNode;
    private BayesNode defTypeNode;
    private JunctionTreeAlgorithm junctionTree;

    private ITypeName typeName;
    private HashMap<IMethodName, BayesNode> callNodes;

    public JayesCallModel(final ITypeName name, final BayesianNetwork network) {
        initalizeIndexes(name);
        initializeNetwork(network);
    }

    private void initalizeIndexes(final ITypeName name) {
        typeName = name;
        callNodes = new HashMap<IMethodName, BayesNode>();
    }

    private void initializeNetwork(final BayesianNetwork network) {
        net = new BayesNet();
        initializeNodes(network);
        initializeArcs(network);
        initializeProbabilities(network);

        junctionTree = new JunctionTreeAlgorithm();
        junctionTree.setNetwork(net);
    }

    private void initializeNodes(final BayesianNetwork network) {
        Collection<Node> nodes = network.getNodes();
        for (Node node : nodes) {
            BayesNode bayesNode = new BayesNode(node.getIdentifier());
            String[] states = node.getStates();
            for (int i = 0; i < states.length; i++) {
                bayesNode.addOutcome(states[i]);
            }
            net.addNode(bayesNode);

            if (node.getIdentifier().equals(N_NODEID_CONTEXT)) {
                overridesNode = bayesNode;
            } else if (node.getIdentifier().equals(N_NODEID_CALL_GROUPS)) {
                callgroupNode = bayesNode;
            } else if (node.getIdentifier().equals(N_NODEID_DEF_KIND)) {
                defTypeNode = bayesNode;
            } else if (node.getIdentifier().equals(N_NODEID_DEF)) {
                definedByNode = bayesNode;
            } else {
                VmMethodName vmMethodName = VmMethodName.get(node.getIdentifier());
                callNodes.put(vmMethodName, bayesNode);
            }
        }
    }

    private void initializeArcs(final BayesianNetwork network) {
        Collection<Node> nodes = network.getNodes();
        for (Node node : nodes) {
            Node[] parents = node.getParents();
            BayesNode children = net.getNode(node.getIdentifier());
            List<BayesNode> bnParents = Lists.newLinkedList();
            for (int i = 0; i < parents.length; i++) {
                bnParents.add(net.getNode(parents[i].getIdentifier()));
            }
            children.setParents(bnParents);
        }
    }

    private void initializeProbabilities(final BayesianNetwork network) {
        Collection<Node> nodes = network.getNodes();
        for (Node node : nodes) {
            BayesNode bayesNode = net.getNode(node.getIdentifier());
            bayesNode.setProbabilities(node.getProbabilities());
        }
    }

    private Optional<IMethodName> computeMethodNameFromState(final BayesNode node) {
        String stateId = junctionTree.getEvidence().get(node);
        if (stateId == null) {
            return absent();
        }
        return Optional.<IMethodName> of(VmMethodName.get(stateId));
    }

    @Override
    public ImmutableSet<IMethodName> getKnownCalls() {
        return ImmutableSet.<IMethodName> builder().addAll(callNodes.keySet()).build();
    }

    @Override
    public ImmutableSet<IMethodName> getKnownOverrideContexts() {
        Collection<IMethodName> tmp = transform(overridesNode.getOutcomes(), new StringToMethodNameFunction());
        return copyOf(tmp);
    }

    @Override
    public ImmutableSet<String> getKnownPatterns() {
        return copyOf(callgroupNode.getOutcomes());
    }

    @Override
    public ImmutableSet<IMethodName> getObservedCalls() {
        Builder<IMethodName> builder = ImmutableSet.<IMethodName> builder();
        Map<BayesNode, String> evidence = junctionTree.getEvidence();
        for (Entry<IMethodName, BayesNode> pair : callNodes.entrySet()) {
            BayesNode node = pair.getValue();
            IMethodName method = pair.getKey();
            if (evidence.containsKey(node) && evidence.get(node).equals(Constants.N_STATE_TRUE)
            // remove the NULL that may have been introduced by
            // res.add(compute...)
                    && !VmMethodName.NULL.equals(method)) {
                builder.add(method);
            }
        }
        return builder.build();
    }

    @Override
    public Optional<IMethodName> getObservedDefiningMethod() {
        return computeMethodNameFromState(definedByNode);
    }

    @Override
    public Optional<IMethodName> getObservedOverrideContext() {
        return computeMethodNameFromState(overridesNode);
    }

    @Override
    public Optional<DefinitionType> getObservedDefinitionType() {
        String stateId = junctionTree.getEvidence().get(defTypeNode);
        if (stateId == null) {
            return absent();
        }
        return of(DefinitionType.valueOf(stateId));
    }

    @Override
    public List<Recommendation<IMethodName>> getRecommendedCalls() {
        List<Recommendation<IMethodName>> recs = Lists.newLinkedList();
        for (IMethodName method : callNodes.keySet()) {
            BayesNode bayesNode = callNodes.get(method);
            boolean isAlreadyUsedAsEvidence = junctionTree.getEvidence().containsKey(bayesNode);
            if (!isAlreadyUsedAsEvidence) {
                int indexForTrue = bayesNode.getOutcomeIndex(N_STATE_TRUE);
                double[] probabilities = junctionTree.getBeliefs(bayesNode);
                double probability = probabilities[indexForTrue];
                recs.add(newRecommendation(method, probability));
            }
        }
        return recs;
    }

    @Override
    public List<Recommendation<IMethodName>> getRecommendedDefinitions() {
        List<Recommendation<IMethodName>> recs = Lists.newLinkedList();
        double[] beliefs = junctionTree.getBeliefs(definedByNode);
        for (int i = definedByNode.getOutcomeCount(); i-- > 0;) {
            if (beliefs[i] > 0.01d) {
                String outcomeName = definedByNode.getOutcomeName(i);
                if (outcomeName.equals("LNone.none()V")) {
                    continue;
                }
                if (outcomeName.equals(UNKNOWN_METHOD.getIdentifier())) {
                    continue;
                }
                VmMethodName definition = VmMethodName.get(outcomeName);
                Recommendation<IMethodName> r = newRecommendation(definition, beliefs[i]);
                recs.add(r);
            }
        }
        return recs;
    }

    @Override
    public List<Recommendation<String>> getRecommendedPatterns() {
        List<Recommendation<String>> recs = Lists.newLinkedList();
        double[] probs = junctionTree.getBeliefs(callgroupNode);
        for (String outcome : callgroupNode.getOutcomes()) {
            int probIndex = callgroupNode.getOutcomeIndex(outcome);
            double p = probs[probIndex];
            recs.add(newRecommendation(outcome, p));
        }
        return recs;
    }

    @Override
    public ITypeName getReceiverType() {
        return typeName;
    }

    @Override
    public void reset() {
        junctionTree.getEvidence().clear();
    }

    @Override
    public boolean setObservedCalls(final Set<IMethodName> calls) {
        for (IMethodName m : getObservedCalls()) {
            // clear previously called methods by setting observation state to NULL
            setCalled(m, null);
        }

        boolean pass = true;
        for (IMethodName m : calls) {
            pass &= setCalled(m, N_STATE_TRUE);
        }
        // explicitly set the "no-method" used node to false:
        pass &= setCalled(Constants.NO_METHOD, N_STATE_FALSE);
        return pass;
    }

    @Override
    public boolean setObservedDefiningMethod(@Nullable final IMethodName newDefinition) {
        if (newDefinition == null) {
            junctionTree.removeEvidence(definedByNode);
            return true;
        }
        // else:
        String identifier = newDefinition.getIdentifier();
        boolean contains = definedByNode.getOutcomes().contains(identifier);
        if (contains) {
            junctionTree.addEvidence(definedByNode, identifier);
        }
        return contains;
    }

    @Override
    public boolean setObservedOverrideContext(@Nullable final IMethodName newEnclosingMethod) {
        if (newEnclosingMethod == null) {
            junctionTree.removeEvidence(overridesNode);
            return true;
        }
        // else:
        String id = newEnclosingMethod.getIdentifier();
        boolean contains = overridesNode.getOutcomes().contains(id);
        if (contains) {
            junctionTree.addEvidence(overridesNode, id);
        }
        return contains;
    }

    @Override
    public boolean setObservedDefinitionType(@Nullable final DefinitionType newDef) {
        if (newDef == null) {
            junctionTree.removeEvidence(defTypeNode);
            return true;
        }
        // else:
        String identifier = newDef.toString();
        boolean contains = defTypeNode.getOutcomes().contains(identifier);
        if (contains) {
            junctionTree.addEvidence(defTypeNode, identifier);
        }
        return contains;
    }

    @Override
    public boolean setObservedPattern(@Nullable final String patternName) {
        if (patternName == null) {
            junctionTree.removeEvidence(callgroupNode);
            return true;
        }
        // else:
        boolean contains = callgroupNode.getOutcomes().contains(patternName);
        if (contains) {
            junctionTree.addEvidence(callgroupNode, patternName);
        }
        return contains;
    }

    private boolean setCalled(final IMethodName m, @Nullable final String state) {
        IMethodName rebased = VmMethodName.rebase(typeName, m);
        BayesNode node = net.getNode(rebased.getIdentifier());
        if (node == null) {
            return false;
        }

        if (state == null) {
            junctionTree.removeEvidence(node);
        } else {
            junctionTree.addEvidence(node, state);
        }
        return true;
    }

}
