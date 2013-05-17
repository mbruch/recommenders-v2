package org.eclipse.recommenders.utils;

import java.util.List;

public interface RecommendationsProcessor<T> {

    List<Recommendation<T>> process(List<Recommendation<T>> recommendations);
}
