package org.eclipse.recommenders.models;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.eclipse.recommenders.utils.Throws;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

/**
 * Represents a Maven-like artifact coordinate which consists of "group-id:artifact-id:classifier:extension:version".
 * This class is used instead of Aether's Artifact class (i) to hide the usage of the Aether from clients, and (ii) to
 * make clear that its only a resource identifier; it does not locate a (resolved) resource.
 */
public final class ModelArchiveCoordinate {

    public static final ModelArchiveCoordinate UNKNOWN = new ModelArchiveCoordinate("unknown",
            "unknown",
            "unknown",
            "unknown",
            "0.0.0");

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;
    private final String extension;

    public ModelArchiveCoordinate(String groupId, String artifactId, String classifier, String extension, String version) {
        this.groupId = Strings.nullToEmpty(groupId);
        this.artifactId = Strings.nullToEmpty(artifactId);
        this.classifier = Strings.nullToEmpty(classifier);
        this.extension = Strings.nullToEmpty(extension);
        this.version = Strings.nullToEmpty(version);
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getExtension() {
        return extension;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, false);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return Joiner.on(':').join(getGroupId(), getArtifactId(), getClassifier(), getExtension(), getVersion());
    }

    public static ModelArchiveCoordinate valueOf(String coord) {
        String[] split = coord.split(":");
        String gid = null, aid = null, ext = null, clss = null, vers = null;
        switch (split.length) {
        case 1:
        case 2:
            throw Throws.throwIllegalArgumentException("Invalid coordinate format. It has only %d segments: %s",
                    split.length,
                    coord);
        case 3:
            vers = split[2];
            break;
        case 4:
            clss = split[2];
            vers = split[3];
            break;
        case 5:
            clss = split[2];
            ext = split[3];
            vers = split[4];
            break;
        default:
            Throws.throwIllegalArgumentException("Invalid coordinate format. It has only %d segments: %s",
                    split.length,
                    coord);
        }
        gid = split[0];
        aid = split[1];
        return new ModelArchiveCoordinate(gid, aid, clss, ext, vers);
    }
}
