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

import java.io.File;

import com.google.common.base.Optional;

/**
 * Represent an element in the workspace containing the corresponding file, type and other attributes.
 */
public interface IDependencyInfo {

    public File getFile();

    public DependencyType getType();

    public Optional<String> getAttribute(String key);

}
