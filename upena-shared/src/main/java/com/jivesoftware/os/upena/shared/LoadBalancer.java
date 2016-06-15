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
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LoadBalancer implements Stored<LoadBalancer>, Serializable {

    public final String name;
    public final String description;
    public final Map<Integer, LoadBalancerListener> listeners;

    @JsonCreator
    public LoadBalancer(@JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("listeners") Map<Integer, LoadBalancerListener> listeners) {
        this.name = name;
        this.description = description;
        this.listeners = listeners;
    }

    @Override
    public String toString() {
        return "Cluster{"
            + "name=" + name
            + ", description=" + description
            + ", listeners=" + listeners
            + '}';
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
        final LoadBalancer other = (LoadBalancer) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(LoadBalancer o) {
        return name.compareTo(o.name);
    }

}
