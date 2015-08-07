package com.jivesoftware.os.upena.deployable.region;

import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.uba.service.RepositoryProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 *
 */
// soy.page.instancesPluginRegion
public class DependenciesPluginRegion implements PageRegion<DependenciesPluginRegion.DependenciesPluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;

    public DependenciesPluginRegion(String template,
        SoyRenderer renderer,
        UpenaStore upenaStore
    ) {
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
    }

    public static class DependenciesPluginRegionInput implements PluginInput {

        final String releaseKey;
        final String release;
        final String action;

        public DependenciesPluginRegionInput(String releaseKey, String release, String action) {
            this.releaseKey = releaseKey;
            this.release = release;
            this.action = action;
        }

        @Override
        public String name() {
            return "Deps";
        }

    }

    @Override
    public String render(String user, DependenciesPluginRegionInput input) {
        Map<String, Object> data = Maps.newHashMap();

        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("releaseKey", input.releaseKey);
            filters.put("release", input.release);
            data.put("filters", filters);

            List<Map<String, Object>> rows = new ArrayList<>();
            ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(new ReleaseGroupKey(input.releaseKey));
            if (releaseGroup != null) {
                deploy(releaseGroup, rows);
            }

            Collections.sort(rows, (Map<String, Object> o1, Map<String, Object> o2) -> {
                int c = ((String) o1.get("groupId")).compareTo((String) o2.get("groupId"));
                if (c != 0) {
                    return c;
                }
                c = ((String) o1.get("artifactId")).compareTo((String) o2.get("artifactId"));
                if (c != 0) {
                    return c;
                }
                return c;
            });
            data.put("dependencies", rows);

        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    private void deploy(ReleaseGroup releaseGroup, List<Map<String, Object>> rows) {

        RepositorySystem system = RepositoryProvider.newRepositorySystem();
        RepositorySystemSession session = RepositoryProvider.newRepositorySystemSession(system);
        String[] repos = releaseGroup.repository.split(",");
        List<RemoteRepository> remoteRepos = RepositoryProvider.newRepositories(system, session, repos);
        String[] deployablecoordinates = releaseGroup.version.trim().split(",");
        gather(deployablecoordinates[0], remoteRepos, system, session, rows);

    }

    private void gather(String deployablecoordinate,
        List<RemoteRepository> remoteRepos,
        RepositorySystem system,
        RepositorySystemSession session,
        List<Map<String, Object>> rows) {

        String[] versionParts = deployablecoordinate.trim().split(":");
        if (versionParts.length != 4) {
            System.out.println("deployable coordinates must be of the following form: groupId:artifactId:packaging:version");
            return;
        }
        String groupId = versionParts[0];
        String artifactId = versionParts[1];
        String packaging = versionParts[2];
        String version = versionParts[3];

        try {
            Artifact artifact = new DefaultArtifact(groupId, artifactId, packaging, version);
            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(new Dependency(artifact, ""));
            collectRequest.setRepositories(remoteRepos);
            CollectResult collectResult = system.collectDependencies(session, collectRequest);
            GatherDependencies gatherDependencies = new GatherDependencies(system, session, remoteRepos, rows);
            collectResult.getRoot().accept(gatherDependencies);

        } catch (DependencyCollectionException ex) {
            LOG.warn("Failed resolving artifact:", ex);
        }

    }

    @Override
    public String getTitle() {
        return "Release Dependencies";
    }

    public class GatherDependencies implements DependencyVisitor {

        private final RepositorySystem system;
        private final RepositorySystemSession session;
        private final List<RemoteRepository> remoteRepos;
        private final List<Map<String, Object>> rows;

        public GatherDependencies(RepositorySystem system,
            RepositorySystemSession session,
            List<RemoteRepository> remoteRepos,
            List<Map<String, Object>> rows) {
            this.system = system;
            this.session = session;
            this.remoteRepos = remoteRepos;
            this.rows = rows;
        }

        @Override
        public boolean visitEnter(DependencyNode node) {
            Artifact artifact = node.getArtifact();
            ArtifactRequest artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(artifact);
            artifactRequest.setRepositories(remoteRepos);

            try {
                ArtifactResult artifactResult = system.resolveArtifact(session, artifactRequest);
                if (artifactResult != null) {
                    artifact = artifactResult.getArtifact();

                    String latest = latest(artifact);
                    Map<String, Object> row = new HashMap<>();
                    row.put("groupId", artifact.getGroupId());
                    row.put("artifactId", artifact.getArtifactId());
                    row.put("versionColor", artifact.getVersion().equals(latest) ? "#75f471" : "#fdda99");
                    row.put("version", artifact.getVersion());
                    row.put("latestVersion", latest);
                    row.put("classifier", artifact.getClassifier());
                    row.put("extension", artifact.getExtension());
                    row.put("isSnapshot", artifact.isSnapshot());

                    rows.add(row);
                }
            } catch (ArtifactResolutionException ex) {
                LOG.warn("Failed to resolve artifact.", ex);
            }
            return true;
        }

        public String latest(Artifact a) throws ArtifactResolutionException {

            Artifact artifact = new DefaultArtifact(a.getGroupId(), a.getArtifactId(), a.getExtension(), "RELEASE");
            ArtifactRequest artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(artifact);
            artifactRequest.setRepositories(remoteRepos);

            ArtifactResult artifactResult = system.resolveArtifact(session, artifactRequest);
            artifact = artifactResult.getArtifact();
            return artifact.getVersion();
        }

        @Override
        public boolean visitLeave(DependencyNode node) {
            return true;
        }
    }
}
