/*
 * Copyright 2016 jonathan.colt.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jivesoftware.os.upena.deployable;

import com.google.common.base.Joiner;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupFilter;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import com.jivesoftware.os.upena.uba.service.RepositoryProvider;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;

/**
 *
 * @author jonathan.colt
 */
public class UpenaAutoRelease {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final RepositoryProvider repositoryProvider;
    private final UpenaStore upenaStore;

    public UpenaAutoRelease(RepositoryProvider repositoryProvider, UpenaStore upenaStore) {
        this.repositoryProvider = repositoryProvider;
        this.upenaStore = upenaStore;
    }

    public void uploaded(File pom) {
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(pom));
            System.out.println("BALLS " + model);

            RepositorySystem system = repositoryProvider.newRepositorySystem();
            DefaultRepositorySystemSession session = repositoryProvider.newRepositorySystemSession(system);
            List<RemoteRepository> repositories = repositoryProvider.newRepositories(system, session, null, (String) null);
            Artifact artifact = new DefaultArtifact(model.getGroupId(), model.getArtifactId(), model.getPackaging(), "(0,]");
            VersionRangeResult resolveVersion = system.resolveVersionRange(session, new VersionRangeRequest(artifact, repositories, null));

            if (resolveVersion != null) {
                System.out.println("BALLS " + model + " " + resolveVersion.getHighestVersion());

                String find = model.getGroupId() + ":" + model.getArtifactId() + ":";
                ReleaseGroupFilter filter = new ReleaseGroupFilter(null, null, find, null, null, 0, 1000);
                System.out.println("BALLS find " + find);

                ConcurrentNavigableMap<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> found = upenaStore.releaseGroups.find(filter);
                System.out.println("BALLS found " + found.size());
                for (Map.Entry<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> entry : found.entrySet()) {
                    ReleaseGroup releaseGroup = entry.getValue().getValue();
                    System.out.println("BALLS found " + releaseGroup);
                    if (releaseGroup.autoRelease) {
                        System.out.println("BALLS " + releaseGroup);

                        String[] deployablecoordinates = releaseGroup.version.trim().split(",");

                        boolean changed = false;
                        StringBuilder newVersion = new StringBuilder();
                        for (String deployablecoordinate : deployablecoordinates) {
                            String[] versionParts = deployablecoordinate.trim().split(":");
                            if (versionParts.length != 4) {
                                LOG.warn("deployable coordinates must be of the following form: groupId:artifactId:packaging:version");
                                if (newVersion.length() != 0) {
                                    newVersion.append(',');
                                }
                                newVersion.append(deployablecoordinate);
                            } else {
                                if (newVersion.length() != 0) {
                                    newVersion.append(',');
                                }
                                // TODO use ComparableVersion and ensure is newer
                                if (!resolveVersion.getHighestVersion().toString().equals(versionParts[versionParts.length - 1])) {

                                    versionParts[versionParts.length - 1] = resolveVersion.getHighestVersion().toString();
                                    newVersion.append(Joiner.on(":").join(versionParts));
                                    changed = true;
                                }
                            }
                        }

                        if (changed) {
                            releaseGroup = new ReleaseGroup(releaseGroup.name, releaseGroup.email, releaseGroup.rollbackVersion, newVersion.toString(),
                                releaseGroup.repository, releaseGroup.description, releaseGroup.autoRelease);

                            upenaStore.releaseGroups.update(entry.getKey(), releaseGroup);

                            upenaStore.record("autoRelease", "updated release", System.currentTimeMillis(), "new version detected", releaseGroup.name,
                                newVersion
                                .toString());
                        }
                    }
                }
            }

        } catch (Exception x) {
            LOG.error("Failed to open " + pom, x);
        }
    }

    private void buildCompleted() {

    }
}
