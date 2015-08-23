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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

class CheckGitInfo {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    public CheckGitInfo() {
    }

    public List<String> gitInfo(String repository, String coordinates) {

        RepositorySystem system = RepositoryProvider.newRepositorySystem();
        RepositorySystemSession session = RepositoryProvider.newRepositorySystemSession(system);
        String[] repos = repository.split(",");
        RepositoryPolicy policy = new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_INTERVAL + ":1", RepositoryPolicy.CHECKSUM_POLICY_WARN);
        List<RemoteRepository> remoteRepos = RepositoryProvider.newRepositories(system, session, policy, repos);

        String[] deployablecoordinates = coordinates.trim().split(",");
        List<String> lines = new ArrayList<>();
        for (String coordinate : deployablecoordinates) {

            for (RemoteRepository repo : remoteRepos) {
                List<String> gitInfo = gitInfo(coordinate, repo, system, session);
                if (gitInfo != null) {
                    lines.addAll(gitInfo);
                }
            }
        }

        return lines;
    }

    private List<String> gitInfo(String deployablecoordinate,
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
            String version = "RELEASE";

            Artifact artifact = new DefaultArtifact(groupId, artifactId, packaging, version);
            ArtifactRequest artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(artifact);
            artifactRequest.setRepositories(Arrays.asList(remoteRepos));

            LOG.info(" Resolving: " + deployablecoordinate);
            ArtifactResult artifactResult = system.resolveArtifact(session, artifactRequest);
            artifact = artifactResult.getArtifact();
            String latestRelease = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + packaging + ":" + artifact.getVersion();
            if (!latestRelease.equals(deployablecoordinate)) {
                LOG.info("There is a newer version of " + deployablecoordinate + " which is " + latestRelease);
            }
            artifact = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), "git", "jar", artifact.getVersion());
            artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(artifact);
            artifactRequest.setRepositories(Arrays.asList(remoteRepos));
            artifactResult = system.resolveArtifact(session, artifactRequest);
            artifact = artifactResult.getArtifact();

            File file = artifact.getFile();
            if (file.exists()) {
                Properties prop = new Properties();
                InputStream input = null;

                try {

                    input = new FileInputStream(file);

                    // load a properties file
                    prop.load(input);

                    List<String> lines = new ArrayList<>();
                    for (Map.Entry<Object, Object> e : prop.entrySet()) {
                        if (e.getKey() instanceof String) {
                            if (((String) e.getKey()).startsWith("git.")) {
                                lines.add(e.getKey() + "=" + e.getValue());
                            }
                        }
                    }
                    return lines;

                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    if (input != null) {
                        try {
                            input.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return null;

        } catch (ArtifactResolutionException x) {
            LOG.warn("Failed to resolve " + deployablecoordinate + " against " + remoteRepos, x);
            return null;
        }

    }

}
