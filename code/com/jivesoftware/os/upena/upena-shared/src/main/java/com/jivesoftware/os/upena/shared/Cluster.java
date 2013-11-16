package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.Objects;

public class Cluster implements Stored<Cluster> {

    public final String name;
    public final String description;
    public final Map<ServiceKey, ReleaseGroupKey> defaultReleaseGroups;

    @JsonCreator
    public Cluster(@JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("defaultReleaseGroups") Map<ServiceKey, ReleaseGroupKey> defaultReleaseGroups) {
        this.name = name;
        this.description = description;
        this.defaultReleaseGroups = defaultReleaseGroups;
    }

    @Override
    public String toString() {
        return "Cluster{" + "name=" + name + ", description=" + description + ", owner=" + defaultReleaseGroups + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Cluster other = (Cluster) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Cluster o) {
        return name.compareTo(o.name);
    }

}
