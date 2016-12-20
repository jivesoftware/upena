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
public class Permission implements Stored<Permission>, Serializable {

    public final String permission;
    public final String description;
    public final long expiration;

    @JsonCreator
    public Permission(@JsonProperty("permission") String permission,
        @JsonProperty("description") String description,
        @JsonProperty("expiration") long expiration) {
        this.permission = permission;
        this.description = description;
        this.expiration = expiration;
    }

    @Override
    public String toString() {
        return "Cluster{"
            + "permission=" + permission
            + ", description=" + description
            + ", expiration=" + expiration
            + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + Objects.hashCode(this.permission);
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
        final Permission other = (Permission) obj;
        if (!Objects.equals(this.permission, other.permission)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Permission o) {
        return permission.compareTo(o.permission);
    }

}
