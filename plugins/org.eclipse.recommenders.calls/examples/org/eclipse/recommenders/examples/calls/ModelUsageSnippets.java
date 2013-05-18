package org.eclipse.recommenders.examples.calls;

import java.util.List;

import org.eclipse.recommenders.calls.ICallModel;
import org.eclipse.recommenders.calls.ICallModel.DefinitionType;
import org.eclipse.recommenders.utils.Recommendation;
import org.eclipse.recommenders.utils.Recommendations;
import org.eclipse.recommenders.utils.names.IMethodName;

import com.google.common.collect.Lists;

public class ModelUsageSnippets {

    List<Recommendation<IMethodName>> recommendCalls(ICallModel m, IMethodName overridesContext, DefinitionType def,
            IMethodName definedBy) {
        m.setObservedDefinitionType(def);
        m.setObservedDefiningMethod(definedBy);
        m.setObservedOverrideContext(overridesContext);

        return m.getRecommendedCalls(Recommendations.<IMethodName> topElementsSortedByRelevance(0.1d, 5));
    }

    List<Recommendation<IMethodName>> recommendDefs(ICallModel m, IMethodName overridesContext) {
        m.setObservedOverrideContext(overridesContext);
        return m.getRecommendedDefinitions(Recommendations.<IMethodName> topElementsSortedByRelevance(0.1d, 5));
    }

    // Given the type of a variable and how it was defined, tell me which "sets of methods" I'm likely to invoke on this
    // object now:
    List<List<Recommendation<IMethodName>>> recommendPatterns(ICallModel m, IMethodName overridesContext,
            DefinitionType def, IMethodName definedBy) {

        m.setObservedOverrideContext(overridesContext);
        m.setObservedDefiningMethod(definedBy);
        m.setObservedDefinitionType(def);

        // get the top 20 most likely call patterns
        List<Recommendation<String>> recommendedPatterns = m.getRecommendedPatterns(Recommendations.<String> topElementsSortedByRelevance(0.01d,
                5));

        List<List<Recommendation<IMethodName>>> res = Lists.newLinkedList();
        // for each pattern, collect which methods it would recommend and insert it into the result set:
        for (Recommendation<String> pattern : recommendedPatterns) {
            m.setObservedPattern(pattern.getProposal());
            List<Recommendation<IMethodName>> callgroup = m.getRecommendedCalls(Recommendations.<IMethodName> topElementsSortedByRelevance(0.1d,
                    20));
            res.add(callgroup);
        }
        return res;
    }

    void javacGenericsCheck(ICallModel m) {
        m.getRecommendedCalls(Recommendations.<IMethodName> topElementsSortedByRelevance(0.1d, 5));
        m.getRecommendedPatterns(Recommendations.<String> topElementsSortedByRelevance(0.1d, 5));
        m.getRecommendedDefinitions(Recommendations.<IMethodName> topElementsSortedByRelevance(0.1d, 5));
    }
}
