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
import java.util.Collections;
import java.util.Map;

import org.eclipse.recommenders.utils.Checks;

import com.google.common.base.Optional;

public class DependencyInfo implements IDependencyInfo {

    public static final String JRE_VERSION_IDE = "JRE_VERSION_IDE";

    private final File file;
    private final DependencyType type;
    private final Map<String, String> attributes;

    public DependencyInfo(File file, DependencyType type) {
        this(file, type, Collections.<String, String> emptyMap());
    }

    public DependencyInfo(File file, DependencyType type, Map<String, String> attributes) {
        this.file = file;
        this.type = type;
        this.attributes = Checks.ensureIsNotNull(attributes);
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public DependencyType getType() {
        return type;
    }

    @Override
    public Optional<String> getAttribute(String key) {
        return Optional.fromNullable(attributes.get(key));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((file == null) ? 0 : file.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DependencyInfo other = (DependencyInfo) obj;
        if (attributes == null) {
            if (other.attributes != null)
                return false;
        } else if (!attributes.equals(other.attributes))
            return false;
        if (file == null) {
            if (other.file != null)
                return false;
        } else if (!file.equals(other.file))
            return false;
        if (type != other.type)
            return false;
        return true;
    }

}
