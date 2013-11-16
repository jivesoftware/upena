package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class ReleaseGroup implements Stored<ReleaseGroup> {

    public final String name;
    public final String email;
    public final String version;
    public final String description;

    @JsonCreator
    public ReleaseGroup(@JsonProperty("name") String name,
            @JsonProperty("email") String email,
            @JsonProperty("version") String version,
            @JsonProperty("description") String description) {
        this.name = name;
        this.email = email;
        this.version = version;
        this.description = description;
    }

    @Override
    public String toString() {
        return "ReleaseGroup{" + "name=" + name + ", email=" + email + ", version=" + version + ", description=" + description + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.name);
        hash = 89 * hash + Objects.hashCode(this.email);
        hash = 89 * hash + Objects.hashCode(this.version);
        hash = 89 * hash + Objects.hashCode(this.description);
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
        final ReleaseGroup other = (ReleaseGroup) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.email, other.email)) {
            return false;
        }
        if (!Objects.equals(this.version, other.version)) {
            return false;
        }
        if (!Objects.equals(this.description, other.description)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(ReleaseGroup o) {
        return email.compareTo(o.email);
    }
}
