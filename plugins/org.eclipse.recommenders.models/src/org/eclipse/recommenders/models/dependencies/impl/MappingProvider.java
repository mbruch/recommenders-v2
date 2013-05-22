/**
 * Copyright (c) 2010, 2013 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Olav Lenz - initial API and implementation
 */
package org.eclipse.recommenders.models.dependencies.impl;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;

import java.util.List;

import org.eclipse.recommenders.models.ProjectCoordinate;
import org.eclipse.recommenders.models.dependencies.DependencyInfo;
import org.eclipse.recommenders.models.dependencies.DependencyType;
import org.eclipse.recommenders.models.dependencies.IMappingProvider;
import org.eclipse.recommenders.models.dependencies.IMappingStrategy;
import org.eclipse.recommenders.utils.annotations.Testing;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.collect.Lists;

public class MappingProvider implements IMappingProvider {

    List<IMappingStrategy> strategies = Lists.newArrayList();
    private Cache<DependencyInfo, Optional<ProjectCoordinate>> cache;

    public MappingProvider() {
        cache = CacheBuilder.newBuilder()
                .maximumSize(200)
                .build(new CacheLoader<DependencyInfo, Optional<ProjectCoordinate>>() {

                    @Override
                    public Optional<ProjectCoordinate> load(DependencyInfo key) throws Exception {

                        return extractProjectCoordinate(key);
                    }
                });
    }

    @Override
    public List<IMappingStrategy> getStrategies() {
        return strategies;
    }

    @Override
    public void addStrategy(IMappingStrategy strategy) {
        strategies.add(strategy);
    }

    @Override
    public void setStrategy(List<IMappingStrategy> strategies) {
        this.strategies = strategies;
    }

    @Override
    public Optional<ProjectCoordinate> searchForProjectCoordinate(DependencyInfo dependencyInfo) {
        try {
            return cache.get(dependencyInfo);
        } catch (Exception e) {
            return absent();
        }
    }

    private Optional<ProjectCoordinate> extractProjectCoordinate(DependencyInfo dependencyInfo) {
        for (IMappingStrategy strategy : strategies) {
            Optional<ProjectCoordinate> optionalProjectCoordinate = strategy.searchForProjectCoordinate(dependencyInfo);
            if (optionalProjectCoordinate.isPresent()) {
                return optionalProjectCoordinate;
            }
        }
        return absent();
    }

    @Override
    public boolean isApplicable(DependencyType dependencyTyp) {
        for (IMappingStrategy mappingStrategy : strategies) {
            if (mappingStrategy.isApplicable(dependencyTyp)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void storeMappings() {
        // TODO: Store mappings when the IDE is closing
    }

    @Override
    public void loadMappings() {
        // TODO: Load mappings when the IDE starts
        // TODO: Needed at least guava 11.0.2, to load stored mappings into the cache.
    }

    @Testing
    public Optional<CacheStats> getCacheStats() {
        return fromNullable(cache.stats());
    }

	@Override
	public void mapManually(DependencyInfo dependencyInfo,
			ProjectCoordinate projectCoordinate) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeMapping(DependencyInfo dependencyInfo) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isManualMapping(DependencyInfo dependencyInfo) {
		// TODO Auto-generated method stub
		return false;
	}

}
