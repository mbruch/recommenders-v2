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
package org.eclipse.recommenders.models.dependencies;

import java.util.List;

import org.eclipse.recommenders.models.ProjectCoordinate;

/**
 * The Mapping interface provide the functionality for the mapping between IDependencyInfo and ProjectCoordinate
 */
public interface IMappingProvider extends IMappingStrategy {

    List<IMappingStrategy> getStrategies();

    void addStrategy(IMappingStrategy strategy);

    void setStrategies(List<IMappingStrategy> strategies);

    void setManualMapping(DependencyInfo dependencyInfo, ProjectCoordinate projectCoordinate);
    
    void removeManualMapping(DependencyInfo dependencyInfo);
    
    boolean isManualMapping(DependencyInfo dependencyInfo);
    
    void storeManualMappings();

    void loadManualMappings();
}
