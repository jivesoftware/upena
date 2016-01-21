/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReleaseGroup implements Stored<ReleaseGroup>, Serializable {

    public final String name;
    public final String email;
    public final String rollbackVersion; // deliberately not part of hash or equals
    public final String version;
    public final String repository;
    public final String description;

    @JsonCreator
    public ReleaseGroup(@JsonProperty("name") String name,
        @JsonProperty("email") String email,
        @JsonProperty("rollbackVersion") String rollbackVersion,
        @JsonProperty("version") String version,
        @JsonProperty("repository") String repository,
        @JsonProperty("description") String description) {
        this.name = name;
        this.email = email;
        this.rollbackVersion = rollbackVersion;
        this.version = version;
        this.repository = repository;
        this.description = description;
    }

    @Override
    public String toString() {
        return "ReleaseGroup{"
            + "name=" + name
            + ", email=" + email
            + ", version=" + version
            + ", repository=" + repository
            + ", description=" + description
            + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.name);
        hash = 89 * hash + Objects.hashCode(this.email);
        hash = 89 * hash + Objects.hashCode(this.version);
        hash = 89 * hash + Objects.hashCode(this.repository);
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
        if (!Objects.equals(this.repository, other.repository)) {
            return false;
        }
        if (!Objects.equals(this.description, other.description)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(ReleaseGroup o) {
        int i = email.compareTo(o.email);
        return i;
    }
}
