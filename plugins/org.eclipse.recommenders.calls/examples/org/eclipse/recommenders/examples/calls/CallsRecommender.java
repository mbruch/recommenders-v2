package org.eclipse.recommenders.examples.calls;

import static org.eclipse.recommenders.utils.Constants.UNKNOWN_METHOD;
import static org.eclipse.recommenders.utils.Recommendations.top;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.eclipse.recommenders.calls.ICallModel;
import org.eclipse.recommenders.calls.ICallModel.DefinitionType;
import org.eclipse.recommenders.calls.OneZipCallModelProvider;
import org.eclipse.recommenders.models.BasedTypeName;
import org.eclipse.recommenders.utils.Constants;
import org.eclipse.recommenders.utils.Recommendation;
import org.eclipse.recommenders.utils.names.IMethodName;
import org.eclipse.recommenders.utils.names.ITypeName;

import com.google.common.collect.Sets;

/**
 * Shows how to create a call recommender that uses a single standalone zipfile containing <b>all</b> models used to
 * create recommendations.
 * <p>
 * The actual recommender is IDE dependent and has to be implemented for every IDE. A call recommenders for code
 * completion, for instance, may perform a lightweight static analysis on the AST of the active editor and then query
 * one or more models for their recommendations.
 */
public class CallsRecommender {

    private final OneZipCallModelProvider store;

    public CallsRecommender(final File models) throws IOException {
        store = new OneZipCallModelProvider(models);
        store.open();
    }

    public List<Recommendation<IMethodName>> computeRecommendations(final ObjectUsage query) throws Exception {
        BasedTypeName name = new BasedTypeName(null, query.type);
        ICallModel net = store.acquireModel(name).orNull();
        try {
            net.setObservedOverrideContext(query.overridesFirst);
            net.setObservedDefinitionType(query.kind);
            if (query.definition != null && !query.definition.equals(UNKNOWN_METHOD)) {
                net.setObservedDefiningMethod(query.definition);
            }
            net.setObservedCalls(query.calls);
            // query the recommender:
            List<Recommendation<String>> patterns = top(net.getRecommendedPatterns(), 10, 0.01);
            List<Recommendation<IMethodName>> definitions = top(net.getRecommendedDefinitions(), 10);
            return top(net.getRecommendedCalls(), 5, 0.01d);

        } finally {
            store.releaseModel(net);
        }
    }

    /**
     * The {@link ObjectUsage} is simple data struct that contains the results of the on-the-fly static analysis done in
     * the IDE at completion time. It's not part of the official API.
     */
    public static class ObjectUsage {

        public static ObjectUsage newObjectUsageWithDefaults() {
            final ObjectUsage res = new ObjectUsage();
            res.type = Constants.UNKNOWN_TYPE;
            res.overridesFirst = Constants.UNKNOWN_METHOD;
            res.overridesSuper = Constants.UNKNOWN_METHOD;
            res.definition = Constants.UNKNOWN_METHOD;
            res.kind = DefinitionType.UNKNOWN;
            return res;
        }

        public ITypeName type;
        public IMethodName overridesSuper;
        public IMethodName overridesFirst;
        public Set<IMethodName> calls = Sets.newHashSet();
        public DefinitionType kind;
        public IMethodName definition;
    }
}
