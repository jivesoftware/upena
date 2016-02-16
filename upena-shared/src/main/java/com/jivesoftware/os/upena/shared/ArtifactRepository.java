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

@JsonIgnoreProperties(ignoreUnknown = true)
public class ArtifactRepository implements Stored<ArtifactRepository>, Serializable {

    public final String name;
    public final String description;
    public final String url;

    @JsonCreator
    public ArtifactRepository(@JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("url") String url
    ) {
        this.name = name;
        this.description = description;
        this.url = url;
    }

    @Override
    public String toString() {
        return "ArtifactRepository{" 
            + "name=" + name
            + ", description=" + description
            + ", url=" + url
            + '}';
    }

    @Override
    public int compareTo(ArtifactRepository o) {
        return name.compareTo(o.name);
    }
}
