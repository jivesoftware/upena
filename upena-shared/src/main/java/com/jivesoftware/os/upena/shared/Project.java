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
public class Project implements Stored<Project>, Serializable {

    public final String name;
    public final String description;
    //public final String localPath;
    //public final String scm;
    //public final String branch;

    //public final String compileScript;
    //public final String testScript;
    //public final String deployScript;

    //public final List<ArtifactRepositoryKey> downloadFromArtifactRepositories;
    //public final List<ArtifactRepositoryKey> uploadToArtifactRepositories;

    //public final List<Project> projectDependencies;

    @JsonCreator
    public Project(@JsonProperty("name") String name,
        @JsonProperty("description") String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String toString() {
        return "Service{"
            + "name=" + name
            + ", description=" + description
            + '}';
    }

    @Override
    public int compareTo(Project o) {
        return name.compareTo(o.name);
    }
}
