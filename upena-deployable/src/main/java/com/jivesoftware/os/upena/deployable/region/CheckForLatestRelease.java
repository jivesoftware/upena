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
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;

class CheckForLatestRelease {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final RepositoryProvider repositoryProvider;
    public CheckForLatestRelease(RepositoryProvider repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
    }

    public LinkedHashMap<String, String> isLatestRelease(String repository, String coordinates) {

        RepositorySystem system = repositoryProvider.newRepositorySystem();
        RepositorySystemSession session = repositoryProvider.newRepositorySystemSession(system);
        String[] repos = repository.split(",");
        RepositoryPolicy policy = new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_INTERVAL + ":1", RepositoryPolicy.CHECKSUM_POLICY_WARN);
        List<RemoteRepository> remoteRepos = repositoryProvider.newRepositories(system, session, policy, repos);

        String[] deployablecoordinates = coordinates.trim().split(",");
        LinkedHashMap<String, String> currentToLatestReleases = new LinkedHashMap<>();
        for (String coordinate : deployablecoordinates) {

            for (RemoteRepository repo : remoteRepos) {
                String latestReleaseCoordinate = checkForLatestRelease(coordinate, repo, system, session);
                if (latestReleaseCoordinate != null) {
                    currentToLatestReleases.put(coordinate, latestReleaseCoordinate);
                }
            }
        }

        return currentToLatestReleases;
    }

    private String checkForLatestRelease(String deployablecoordinate,
        RemoteRepository remoteRepos,
        RepositorySystem system,
        RepositorySystemSession session) {
        String[] versionParts = deployablecoordinate.trim().split(":");
        if (versionParts.length != 4) {
            LOG.warn("deployable coordinates must be of the following form: groupId:artifactId:packaging:version");
            return null;
        }
        try {
            String groupId = versionParts[0];
            String artifactId = versionParts[1];
            String packaging = versionParts[2];
            String version = versionParts[3];

            Artifact artifact = new DefaultArtifact(groupId + ":" + artifactId + ":" + packaging + ":[" + version + ",)");

            VersionRangeRequest rangeRequest = new VersionRangeRequest();
            rangeRequest.setArtifact(artifact);
            rangeRequest.addRepository(remoteRepos);

            VersionRangeResult rangeResult = system.resolveVersionRange(session, rangeRequest);

            List<Version> versions = rangeResult.getVersions();
            for (Version v : versions) {
                LOG.info("There is a newer version of " + deployablecoordinate + " which is " + v);
            }

            if (versions.size() > 1) {
                return groupId + ":" + artifactId + ":" + packaging + ":" + versions.get(versions.size() - 1).toString();
            } else {
                return groupId + ":" + artifactId + ":" + packaging + ":" + version;
            }

        } catch (Exception x) {
            LOG.warn("Failed to resolve " + deployablecoordinate + " against " + remoteRepos, x);
            return null;
        }

    }

}
