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
package com.jivesoftware.os.upena.deployable.region;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.uba.service.RepositoryProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

class CheckReleasable {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final RepositoryProvider repositoryProvider;
    public CheckReleasable(RepositoryProvider repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
    }

    public List<String> isReleasable(String repository, String coordinates) {

        RepositorySystem system = repositoryProvider.newRepositorySystem();
        RepositorySystemSession session = repositoryProvider.newRepositorySystemSession(system);
        String[] repos = repository.split(",");
        List<RemoteRepository> remoteRepos = repositoryProvider.newRepositories(system, session, null, repos);

        String[] deployablecoordinates = coordinates.trim().split(",");
        List<String> errors = new ArrayList<>();
        FOUND:
        for (String coordinate : deployablecoordinates) {

            List<String> possibleErrors = new ArrayList<>();
            for (RemoteRepository repo : remoteRepos) {
                String releasable = releasable(coordinate, repo, system, session);
                if (releasable != null) {
                    possibleErrors.add(releasable);
                } else {
                    continue FOUND;
                }
            }
            errors.addAll(possibleErrors);

        }
        return errors;
    }

    private String releasable(String deployablecoordinate,
        RemoteRepository remoteRepos,
        RepositorySystem system,
        RepositorySystemSession session) {
        String[] versionParts = deployablecoordinate.trim().split(":");
        if (versionParts.length != 4) {
            return "deployable coordinates must be of the following form: groupId:artifactId:packaging:version";
        }
        try {
            String groupId = versionParts[0];
            String artifactId = versionParts[1];
            String packaging = versionParts[2];
            String version = versionParts[3];

            Artifact artifact = new DefaultArtifact(groupId, artifactId, packaging, version);
            ArtifactRequest artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(artifact);
            artifactRequest.setRepositories(Arrays.asList(remoteRepos));

            LOG.info(" Resolving: " + deployablecoordinate);
            ArtifactResult artifactResult = system.resolveArtifact(session, artifactRequest);
            artifact = artifactResult.getArtifact();
            String release = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + packaging + ":" + artifact.getVersion();
            if (release.equals(deployablecoordinate)) {
                return null;
            } else {
                return "Artifacts don't align:" + deployablecoordinate + " vs " + release;
            }

        } catch (ArtifactResolutionException x) {
            return "Failed to resolve " + deployablecoordinate + " against " + remoteRepos + ". Cause:" + x;
        }

    }

}
