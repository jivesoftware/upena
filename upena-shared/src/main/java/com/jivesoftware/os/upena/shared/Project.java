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
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Project implements Stored<Project>, Serializable {

    public final String name;
    public final String description;
    public final String localPath;

    public final String scmUrl;
    public final String branch;

    public final String pom;
    public final String goals;

    public final String mvnHome;
    public final String localPathToRepo;

    public final Set<ArtifactRepositoryKey> downloadFromArtifactRepositories;
    public final Set<ArtifactRepositoryKey> uploadToArtifactRepositories;

    public final List<Project> dependantProjects;

    @JsonCreator
    public Project(@JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("localPath") String localPath,
        @JsonProperty("scmUrl") String scmUrl,
        @JsonProperty("branch") String branch,
        @JsonProperty("pom") String pom,
        @JsonProperty("goals") String goals,
        @JsonProperty("mvnHome") String mvnHome,
        @JsonProperty("localPathToRepo") String localPathToRepo,
        @JsonProperty("downloadFromArtifactRepositories") Set<ArtifactRepositoryKey> downloadFromArtifactRepositories,
        @JsonProperty("uploadToArtifactRepositories") Set<ArtifactRepositoryKey> uploadToArtifactRepositories,
        @JsonProperty("dependantProjects") List<Project> dependantProjects
    ) {
        this.name = name;
        this.description = description;
        this.localPath = localPath;
        this.scmUrl = scmUrl;
        this.branch = branch;
        this.pom = pom;
        this.goals = goals;
        this.mvnHome = mvnHome;
        this.localPathToRepo = localPathToRepo;
        this.downloadFromArtifactRepositories = downloadFromArtifactRepositories;
        this.uploadToArtifactRepositories = uploadToArtifactRepositories;
        this.dependantProjects = dependantProjects;
    }

    @Override
    public String toString() {
        return "Project{"
            + "name=" + name
            + ", description=" + description
            + ", localPath=" + localPath
            + ", scmUrl=" + scmUrl
            + ", branch=" + branch
            + ", pom=" + pom
            + ", goals=" + goals
            + ", mvnHome=" + mvnHome
            + ", localPathToRepo=" + localPathToRepo
            + ", downloadFromArtifactRepositories=" + downloadFromArtifactRepositories
            + ", uploadToArtifactRepositories=" + uploadToArtifactRepositories
            + ", dependantProjects=" + dependantProjects
            + '}';
    }

    @Override
    public int compareTo(Project o) {
        return name.compareTo(o.name);
    }
}
