package org.eclipse.recommenders.examples.calls;

import static org.eclipse.recommenders.utils.Recommendations.*;

import java.util.List;

import org.eclipse.recommenders.calls.ICallModel;
import org.eclipse.recommenders.calls.ICallModel.DefinitionType;
import org.eclipse.recommenders.utils.Recommendation;
import org.eclipse.recommenders.utils.names.IMethodName;

import com.google.common.collect.Lists;

public class ModelUsageSnippets {

    List<Recommendation<IMethodName>> recommendCalls(ICallModel m, IMethodName overridesContext, DefinitionType def,
            IMethodName definedBy) {
        m.setObservedDefinitionType(def);
        m.setObservedDefiningMethod(definedBy);
        m.setObservedOverrideContext(overridesContext);

        List<Recommendation<IMethodName>> recs = m.getRecommendedCalls();

        // top 5 elements with min probablity of 0.1d:
        List<Recommendation<IMethodName>> top5 = top(filterRelevance(recs, 0.1d), 5);
        return top5;
    }

    List<Recommendation<IMethodName>> recommendDefs(ICallModel m, IMethodName overridesContext) {
        m.setObservedOverrideContext(overridesContext);
        List<Recommendation<IMethodName>> recs = m.getRecommendedDefinitions();

        // top 5 elements with min probablity of 0.1d:
        List<Recommendation<IMethodName>> top5 = top(filterRelevance(recs, 0.1d), 5);
        return top5;
    }

    // Given the type of a variable and how it was defined, tell me which "sets of methods" I'm likely to invoke on this
    // object now:
    List<List<Recommendation<IMethodName>>> recommendPatterns(ICallModel m, IMethodName overridesContext,
            DefinitionType def, IMethodName definedBy) {

        m.setObservedOverrideContext(overridesContext);
        m.setObservedDefiningMethod(definedBy);
        m.setObservedDefinitionType(def);

        // get the top 5 most likely call patterns
        List<Recommendation<String>> patterns = top(m.getRecommendedPatterns(), 5);
        List<List<Recommendation<IMethodName>>> res = Lists.newLinkedList();
        // for each pattern, collect which methods it would recommend and insert it into the result set:
        for (Recommendation<String> pattern : patterns) {
            m.setObservedPattern(pattern.getProposal());
            List<Recommendation<IMethodName>> callgroup = top(m.getRecommendedCalls(), 10);
            res.add(callgroup);
        }
        return res;
    }

    void javacGenericsCheck(ICallModel m) {
        top(filterRelevance(m.getRecommendedCalls(), 0.1d), 5);
        // OR shorthand version:
        top(m.getRecommendedCalls(), 5, 0.1d);

        // filter top 5 methods with >=0.1 relevance and NOT being void:
        top(filterVoid(m.getRecommendedCalls()), 5, 0.1d);

        // check for compiler warnings:
        top(filterRelevance(m.getRecommendedPatterns(), 0.2d), 5);
        top(m.getRecommendedPatterns(), 5, 0.2d);
        top(filterRelevance(m.getRecommendedDefinitions(), 0.3d), 5);
        top(m.getRecommendedDefinitions(), 5, 0.2d);
    }
}
