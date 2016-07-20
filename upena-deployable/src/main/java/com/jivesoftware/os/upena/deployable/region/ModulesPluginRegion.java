package com.jivesoftware.os.upena.deployable.region;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.region.ModulesPluginRegion.ModulesPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupFilter;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import com.jivesoftware.os.upena.uba.service.RepositoryProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
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
public class ModulesPluginRegion implements PageRegion<ModulesPluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RepositoryProvider repositoryProvider;
    private final String template;
    private final SoyRenderer renderer;
    private final UpenaStore upenaStore;

    private final ConcurrentHashMap<Artifact, List<Artifact>> deps = new ConcurrentHashMap<>();

    public ModulesPluginRegion(RepositoryProvider repositoryProvider,
        String template,
        SoyRenderer renderer,
        UpenaStore upenaStore
    ) {
        this.repositoryProvider  = repositoryProvider;
        this.template = template;
        this.renderer = renderer;
        this.upenaStore = upenaStore;
    }

    @Override
    public String getRootPath() {
        return "/ui/modules";
    }

    public static class ModulesPluginRegionInput implements PluginInput {

        final String clusterKey;
        final String cluster;
        final String hostKey;
        final String host;
        final String serviceKey;
        final String service;
        final String releaseKey;
        final String release;

        public ModulesPluginRegionInput(String clusterKey, String cluster, String hostKey, String host, String serviceKey, String service,
            String releaseKey, String release) {
            this.clusterKey = clusterKey;
            this.cluster = cluster;
            this.hostKey = hostKey;
            this.host = host;
            this.serviceKey = serviceKey;
            this.service = service;
            this.releaseKey = releaseKey;
            this.release = release;
        }

        @Override
        public String name() {
            return "Modules";
        }

    }

    static class ArtifactKey {

        private final Artifact artifact;

        public ArtifactKey(Artifact artifact) {
            this.artifact = artifact;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 17 * hash + Objects.hashCode(this.artifact.getArtifactId());
            hash = 17 * hash + Objects.hashCode(this.artifact.getGroupId());
            hash = 17 * hash + Objects.hashCode(this.artifact.getVersion());
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ArtifactKey other = (ArtifactKey) obj;
            if (!Objects.equals(this.artifact.getGroupId(), other.artifact.getGroupId())) {
                return false;
            }
            if (!Objects.equals(this.artifact.getVersion(), other.artifact.getVersion())) {
                return false;
            }
            if (!Objects.equals(this.artifact.getArtifactId(), other.artifact.getArtifactId())) {
                return false;
            }
            return true;
        }

    }

    @Override
    public String render(String user, ModulesPluginRegionInput input) {
        Map<String, Object> data = Maps.newHashMap();

        Graph graph = new Graph();

        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("clusterKey", input.clusterKey);
            filters.put("cluster", input.cluster);
            filters.put("hostKey", input.hostKey);
            filters.put("host", input.host);
            filters.put("serviceKey", input.serviceKey);
            filters.put("service", input.service);
            filters.put("releaseKey", input.releaseKey);
            filters.put("release", input.release);
            data.put("filters", filters);

            InstanceFilter filter = new InstanceFilter(
                input.clusterKey.isEmpty() ? null : new ClusterKey(input.clusterKey),
                input.hostKey.isEmpty() ? null : new HostKey(input.hostKey),
                input.serviceKey.isEmpty() ? null : new ServiceKey(input.serviceKey),
                input.releaseKey.isEmpty() ? null : new ReleaseGroupKey(input.releaseKey),
                null,
                0, 100_000);

            Set<ReleaseGroup> releaseGroups = new HashSet<>();

            Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
            for (Map.Entry<InstanceKey, TimestampedValue<Instance>> f : found.entrySet()) {
                if (!f.getValue().getTombstoned()) {
                    Instance instance = f.getValue().getValue();
                    ReleaseGroup got = upenaStore.releaseGroups.get(instance.releaseGroupKey);
                    if (got != null) {
                        releaseGroups.add(got);
                    }
                }
            }

            if (releaseGroups.isEmpty()) {

                ReleaseGroupFilter releaseGroupFilter = new ReleaseGroupFilter(null, null, null, null, null, 0, 100_000);
                ConcurrentNavigableMap<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> foundGroups = upenaStore.releaseGroups.find(releaseGroupFilter);
                for (Map.Entry<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> fg : foundGroups.entrySet()) {
                    if (!fg.getValue().getTombstoned()) {
                        releaseGroups.add(fg.getValue().getValue());
                    }
                }
            }

            RepositorySystem system = repositoryProvider.newRepositorySystem();
            RepositorySystemSession session = repositoryProvider.newRepositorySystemSession(system);

            HashSet<Artifact> gathered = new HashSet<>();
            for (ReleaseGroup releaseGroup : releaseGroups) {

                String[] repos = releaseGroup.repository.split(",");
                List<RemoteRepository> remoteRepos = repositoryProvider.newRepositories(system, session, null, repos);
                String[] deployablecoordinates = releaseGroup.version.trim().split(",");

                HashSet<Artifact> gatherable = new HashSet<>();
                for (String deployablecoordinate : deployablecoordinates) {
                    gather(gathered,
                        gatherable,
                        deployablecoordinate,
                        true,
                        Arrays.asList(new String[]{"com.jivesoftware"}),
                        remoteRepos,
                        system,
                        session,
                        graph);
                }

//                    for (Artifact artifact : gatherable) {
//                        gather(gathered,
//                            new HashSet<>(),
//                            artifact.toString(),
//                            false,
//                            Arrays.asList(new String[]{"com.jivesoftware"}),
//                            remoteRepos,
//                            system,
//                            session,
//                            graph);
//                    }
            }

            List<Map<String, String>> renderNodes = new ArrayList<>();
            for (Node n : graph.nodes.values()) {
                Map<String, String> node = new HashMap<>();
                node.put("id", "id" + n.id);
                if (n.tooltip != null) {
                    node.put("tooltip", n.tooltip);
                }
                node.put("fontSize", n.fontSize);
                if (n.icon != null) {
                    node.put("icon", n.icon);
                }
                if (n.label != null) {
                    node.put("label", n.label);
                }
                node.put("focusHtml", n.focusHtml);
                node.put("color", n.bgcolor);
                node.put("iconSize", n.iconSize);

                renderNodes.add(node);
            }

            data.put("moduleNodes", MAPPER.writeValueAsString(renderNodes));

            List<Map<String, String>> renderEdges = new ArrayList<>();
            for (Edge e : graph.edges.values()) {
                Map<String, String> edge = new HashMap<>();
                edge.put("from", "id" + e.from);
                edge.put("label", e.label);
                edge.put("to", "id" + e.to);
                edge.put("color", e.edgeColor);
                renderEdges.add(edge);
                //System.out.println("edge:" + edge);
            }

            data.put("moduleEdges", MAPPER.writeValueAsString(renderEdges));

        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    private void gather(HashSet<Artifact> gathered,
        HashSet<Artifact> gatherable,
        String deployablecoordinate,
        boolean isDeployable,
        List<String> groupIdPrefixes,
        List<RemoteRepository> remoteRepos,
        RepositorySystem system,
        RepositorySystemSession session,
        Graph graph) {

        String[] versionParts = deployablecoordinate.trim().split(":");
        if (versionParts.length != 4) {
            LOG.warn("deployable coordinates must be of the following form: groupId:artifactId:packaging:version {}", deployablecoordinate);
            return;
        }
        String groupId = versionParts[0];
        String artifactId = versionParts[1];
        String packaging = versionParts[2];
        String version = versionParts[3];

        Artifact artifact = new DefaultArtifact(groupId, artifactId, packaging, version);
        Node fan = graph.artifact(artifact, isDeployable);
        Edge edge = graph.edge(fan, fan);
        edge.label = fan.version;

        Node fgn = graph.group(artifact);
        graph.edge(fan, fgn);

        List<Artifact> ds = deps.computeIfAbsent(artifact, (Artifact a) -> {

            for (RemoteRepository remoteRepo : remoteRepos) {
                try {
                    CollectRequest collectRequest = new CollectRequest();
                    collectRequest.setRoot(new Dependency(a, null));
                    collectRequest.setRepositories(remoteRepos);
                    CollectResult collectResult = system.collectDependencies(session, collectRequest);
                    GatherDependencies gatherDependencies = new GatherDependencies(system, session, remoteRepos);
                    collectResult.getRoot().accept(gatherDependencies);
                    return gatherDependencies.deps;
                } catch (DependencyCollectionException ex) {
                    LOG.warn("Failed resolving artifact:" + a + " in " + remoteRepo);
                }
            }
            return null;

        });
        for (Artifact d : ds) {
            if (artifact.equals(d)) {
                continue;
            }
            //System.out.println(artifact + "->" + d);
            if (groupIdPrefixes.isEmpty()) {
                //Node an = graph.artifact(d, false);
                Node gn = graph.group(d);
                //graph.edge(an, gn);
                edge = graph.edge(fgn, gn);
                edge.edgeColor = fgn.bgcolor;

            } else {
                for (String p : groupIdPrefixes) {
                    if (d.getGroupId().startsWith(p)) {
                        //Node an = graph.artifact(d, false);
                        Node gn = graph.group(d);
                        //graph.edge(an, gn);
                        edge = graph.edge(fgn, gn);
                        edge.edgeColor = fgn.bgcolor;
                        gatherable.add(d);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public String getTitle() {
        return "Build";
    }

    public class GatherDependencies implements DependencyVisitor {

        private final RepositorySystem system;
        private final RepositorySystemSession session;
        private final List<RemoteRepository> remoteRepos;
        public List<Artifact> deps = new ArrayList<>();

        public GatherDependencies(RepositorySystem system,
            RepositorySystemSession session,
            List<RemoteRepository> remoteRepos) {
            this.system = system;
            this.session = session;
            this.remoteRepos = remoteRepos;
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
                    deps.add(artifact);
                }
            } catch (ArtifactResolutionException ex) {
                LOG.warn("Failed to resolve artifact.", ex);
            }
            return true;
        }

        @Override
        public boolean visitLeave(DependencyNode node) {
            return true;
        }
    }

    public static class Graph {

        Map<String, Node> nodes = new ConcurrentHashMap<>();
        Map<String, Edge> edges = new ConcurrentHashMap<>();

        public Node group(Artifact artifact) {
            String key = artifact.getGroupId();
            Node node = nodes.computeIfAbsent(key, (String t) -> {
                String title = artifact.getGroupId().substring(artifact.getGroupId().lastIndexOf(".") + 1);
                Node n = new Node(title, t, randomColor(title.hashCode()), "22");
                n.icon = "group";
                n.tooltip = key;
                return n;

            });
            return node;
        }

        public Node artifact(Artifact artifact, boolean isDeployable) {
            String key = artifact.getGroupId() + ":" + artifact.getArtifactId();
            Node node = nodes.computeIfAbsent(key, (String t) -> {
                Node n = new Node(isDeployable ? artifact.getArtifactId() : "", t, isDeployable ? "99f" : "9f9", "22");
                n.icon = "lib";
                n.version = artifact.getVersion();
                n.tooltip = artifact.toString().replace(':', '\n');
                return n;

            });
            if (isDeployable) {
                node.icon = "deployable";
                node.iconSize = "32";
            }
            return node;
        }

        public Edge edge(Node from, Node to) {
            Edge edge = edges.computeIfAbsent(from.id + "->" + to.id, (String key) -> {
                Edge e = new Edge(from.id, to.id, to.version);
                return e;
            });
            return edge;
        }
    }

    private static final String[] colorParts = {"6", "7", "8", "9", "a", "b", "c", "d", "e"};

    public static String randomColor(int seed) {
        Random r = new Random(seed);
        return colorParts[r.nextInt(colorParts.length)] + colorParts[r.nextInt(colorParts.length)] + colorParts[r.nextInt(colorParts.length)];
    }

    public static class Node {

        String label;
        String icon;
        String id;
        String bgcolor;
        String fontSize;
        String focusHtml;
        String tooltip;
        String version = "";
        String iconSize = "24";

        public Node(String label, String id, String bgcolor, String fontSize) {
            this.label = label;
            this.id = id;
            this.bgcolor = bgcolor;
            this.fontSize = fontSize;
        }

        @Override
        public String toString() {
            return "Node{" + "label=" + label + ", id=" + id + ", bgcolor=" + bgcolor + '}';
        }

    }

    public static class Edge {

        String from;
        String label;
        String to;
        String edgeColor = "000";

        public Edge(String from, String to, String label) {
            this.from = from;
            this.to = to;
            this.label = label;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + Objects.hashCode(this.from);
            hash = 37 * hash + Objects.hashCode(this.to);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Edge other = (Edge) obj;
            if (!Objects.equals(this.from, other.from)) {
                return false;
            }
            if (!Objects.equals(this.to, other.to)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Edge{"
                + "from=" + from
                + ", label=" + label
                + ", to=" + to
                + '}';
        }

    }
}
