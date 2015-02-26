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
import java.util.LinkedHashMap;
import java.util.List;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

class CheckForLatestRelease {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    public CheckForLatestRelease() {
    }

    public LinkedHashMap<String, String> isLatestRelease(String repository, String coordinates) {

        RepositorySystem system = RepositoryProvider.newRepositorySystem();
        RepositorySystemSession session = RepositoryProvider.newRepositorySystemSession(system);
        String[] repos = repository.split(",");
        List<RemoteRepository> remoteRepos = RepositoryProvider.newRepositories(system, session, repos);

        String[] deployablecoordinates = coordinates.trim().split(",");
        LinkedHashMap<String, String> currentToLatestReleases = new LinkedHashMap<>();
        for (String coordinate : deployablecoordinates) {
            String latestReleaseCoordinate = checkForLatestRelease(coordinate, remoteRepos, system, session);
            currentToLatestReleases.put(coordinate, latestReleaseCoordinate);
        }

        return currentToLatestReleases;
    }

    private String checkForLatestRelease(String deployablecoordinate,
        List<RemoteRepository> remoteRepos,
        RepositorySystem system,
        RepositorySystemSession session) {
        String[] versionParts = deployablecoordinate.trim().split(":");
        if (versionParts.length != 4) {
            LOG.warn("deployable coordinates must be of the following form: groupId:artifactId:packaging:version");
            return "Invalid coordinate:" + deployablecoordinate + " expected: groupId:artifactId:packaging:version";
        }
        try {
            String groupId = versionParts[0];
            String artifactId = versionParts[1];
            String packaging = versionParts[2];
            String version = "RELEASE";

            Artifact artifact = new DefaultArtifact(groupId, artifactId, packaging, version);
            ArtifactRequest artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(artifact);
            artifactRequest.setRepositories(remoteRepos);

            LOG.info(" Resolving: " + deployablecoordinate);
            ArtifactResult artifactResult = system.resolveArtifact(session, artifactRequest);
            artifact = artifactResult.getArtifact();
            String latestRelease = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + packaging + ":" + artifact.getVersion();
            if (!latestRelease.equals(deployablecoordinate)) {
                LOG.info("There is a newer version of " + deployablecoordinate + " which is " + latestRelease);
            }
            return latestRelease;

        } catch (ArtifactResolutionException x) {
            return "Failed to resolve:" + deployablecoordinate + " Cause:" + x.getMessage();
        }

    }

}
