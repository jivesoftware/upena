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
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Project implements Stored<Project>, Serializable {

    public final String name;
    public String description;
    public String localPath;

    public String scmUrl;
    public String branch;

    public String pom;
    public String goals;

    public String mvnHome;

    public final Set<ArtifactRepositoryKey> downloadFromArtifactRepositories;
    public final Set<ArtifactRepositoryKey> uploadToArtifactRepositories;

    public Map<String, Map<ProjectKey, Artifact>> conflictingCoordinateProjectKeyArtifacts;
    public Map<ReleaseGroupKey, Artifact> dependantReleaseGroups;

    @JsonCreator
    public Project(@JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("localPath") String localPath,
        @JsonProperty("scmUrl") String scmUrl,
        @JsonProperty("branch") String branch,
        @JsonProperty("pom") String pom,
        @JsonProperty("goals") String goals,
        @JsonProperty("mvnHome") String mvnHome,
        @JsonProperty("downloadFromArtifactRepositories") Set<ArtifactRepositoryKey> downloadFromArtifactRepositories,
        @JsonProperty("uploadToArtifactRepositories") Set<ArtifactRepositoryKey> uploadToArtifactRepositories,
        @JsonProperty("conflictingCoordinateProjectKeyArtifacts") Map<String, Map<ProjectKey, Artifact>> conflictingCoordinateProjectKeyArtifacts,
        @JsonProperty("dependantReleaseGroups") Map<ReleaseGroupKey, Artifact> dependantReleaseGroups
    ) {
        this.name = name;
        this.description = description;
        this.localPath = localPath;
        this.scmUrl = scmUrl;
        this.branch = branch;
        this.pom = pom;
        this.goals = goals;
        this.mvnHome = mvnHome;
        this.downloadFromArtifactRepositories = downloadFromArtifactRepositories;
        this.uploadToArtifactRepositories = uploadToArtifactRepositories;
        this.conflictingCoordinateProjectKeyArtifacts = conflictingCoordinateProjectKeyArtifacts;
        this.dependantReleaseGroups = dependantReleaseGroups;
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
            + ", downloadFromArtifactRepositories=" + downloadFromArtifactRepositories
            + ", uploadToArtifactRepositories=" + uploadToArtifactRepositories
            + ", conflictingCoordinateProjectKeyArtifacts=" + conflictingCoordinateProjectKeyArtifacts
            + ", dependantReleaseGroups=" + dependantReleaseGroups
            + '}';
    }

    @Override
    public int compareTo(Project o) {
        return name.compareTo(o.name);
    }
}
