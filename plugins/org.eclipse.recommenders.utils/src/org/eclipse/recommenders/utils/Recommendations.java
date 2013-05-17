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
package org.eclipse.recommenders.utils;

import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Lists.newLinkedList;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.recommenders.utils.names.IMethodName;

import com.google.common.annotations.Beta;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Ordering;

@Beta
public class Recommendations {

    public static <T> RecommendationsProcessor<T> topElementsSortedByRelevance(double minRelevance, int maxElement) {
        return new RecommendationsProcessor<T>() {

            @Override
            public List<Recommendation<T>> process(List<Recommendation<T>> original) {
                Predicate<Recommendation<?>> filter = filterByMinRelevance(0.1d);
                Comparator<Recommendation<?>> comparator = compareByRelevance();
                TopElementsSelector<T> topElements = topElements(5);

                Collection<Recommendation<T>> tmp = filter(original, filter);
                List<Recommendation<T>> filtered = newLinkedList(tmp);
                Collections.sort(filtered, comparator);
                return filtered.subList(0, topElements.select(filtered));
            }
        };
    }

    public static <T> RecommendationsProcessor<T> defaultProcessor() {
        return topElementsSortedByRelevance(0.01d, 20);
    }

    public static <T> RecommendationsProcessor<T> asIs() {
        return new RecommendationsProcessor<T>() {

            @Override
            public List<Recommendation<T>> process(List<Recommendation<T>> original) {
                return original;
            }
        };
    }

    public static interface TopElementsSelector<T> {
        int select(List<Recommendation<T>> filtered);
    }

    public static <T> TopElementsSelector<T> topElements(final int amount) {
        return new TopElementsSelector<T>() {

            @Override
            public int select(List<Recommendation<T>> recommendations) {
                return Math.min(amount, recommendations.size());
            }

        };
    }

    public static Predicate<Recommendation<?>> filterByMinRelevance(final double min) {
        return new Predicate<Recommendation<?>>() {

            @Override
            public boolean apply(Recommendation<?> r) {
                return r.getRelevance() >= min;
            }
        };
    }

    public static Predicate<Recommendation<?>> filterAny() {
        return Predicates.alwaysTrue();
    };

    /**
     * Returns a Predicate that filters all void methods.
     */
    public static Predicate<Recommendation<IMethodName>> filterVoid() {
        return new Predicate<Recommendation<IMethodName>>() {

            @Override
            public boolean apply(Recommendation<IMethodName> input) {
                return !input.getProposal().isVoid();
            }
        };
    };

    public static Comparator<Recommendation<?>> compareArbitrary() {
        return new Comparator<Recommendation<?>>() {

            @Override
            public int compare(Recommendation<?> o1, Recommendation<?> o2) {
                return o2.hashCode() - o1.hashCode();
            }
        };
    }

    public static Comparator<Recommendation<?>> compareToString() {
        return new Comparator<Recommendation<?>>() {

            @Override
            public int compare(Recommendation<?> o1, Recommendation<?> o2) {
                return o1.toString().compareTo(o2.toString());
            }
        };
    }

    public static Comparator<Recommendation<?>> compareByRelevance() {
        return new Comparator<Recommendation<?>>() {

            @Override
            public int compare(Recommendation<?> o1, Recommendation<?> o2) {
                int res = -1 * Double.compare(o1.getRelevance(), o2.getRelevance());
                return res != 0 ? res : Ordering.usingToString().compare(o1, o2);
            }
        };
    }
}
