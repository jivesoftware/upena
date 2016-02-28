package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 *
 * @author jonathan.colt
 */
public class Artifact {

    public final String groupId;
    public final String artifactId;
    public final String classifier;
    public final String version;
    public final String scope;

    @JsonCreator
    public Artifact(@JsonProperty("groupId") String groupId,
        @JsonProperty("artifactId") String artifactId,
        @JsonProperty("classifier") String classifier,
        @JsonProperty("version") String version,
        @JsonProperty("scope") String scope) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier;
        this.version = version;
        this.scope = scope;
    }

    public Artifact(String[] coordinate) {
        this.groupId = coordinate[0];
        this.artifactId = coordinate[1];
        this.classifier = coordinate[2];
        this.version = coordinate[3];
        this.scope = (coordinate.length <= 4) ? null : coordinate[4];
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + classifier + ":" + version + ((scope != null) ? ":" + scope : "");
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.groupId);
        hash = 41 * hash + Objects.hashCode(this.artifactId);
        hash = 41 * hash + Objects.hashCode(this.classifier);
        hash = 41 * hash + Objects.hashCode(this.version);
        hash = 41 * hash + Objects.hashCode(this.scope);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Artifact other = (Artifact) obj;
        if (!Objects.equals(this.groupId, other.groupId)) {
            return false;
        }
        if (!Objects.equals(this.artifactId, other.artifactId)) {
            return false;
        }
        if (!Objects.equals(this.classifier, other.classifier)) {
            return false;
        }
        if (!Objects.equals(this.version, other.version)) {
            return false;
        }
        if (!Objects.equals(this.scope, other.scope)) {
            return false;
        }
        return true;
    }

}
