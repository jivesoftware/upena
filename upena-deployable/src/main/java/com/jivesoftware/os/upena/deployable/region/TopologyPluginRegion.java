package com.jivesoftware.os.upena.deployable.region;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.jivesoftware.os.amza.shared.AmzaInstance;
import com.jivesoftware.os.amza.shared.RingHost;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.http.client.HttpClient;
import com.jivesoftware.os.routing.bird.http.client.HttpClientConfig;
import com.jivesoftware.os.routing.bird.http.client.HttpClientConfiguration;
import com.jivesoftware.os.routing.bird.http.client.HttpClientFactory;
import com.jivesoftware.os.routing.bird.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import com.jivesoftware.os.routing.bird.shared.ConnectionHealth;
import com.jivesoftware.os.routing.bird.shared.HostPort;
import com.jivesoftware.os.routing.bird.shared.InstanceConnectionHealth;
import com.jivesoftware.os.upena.deployable.UpenaEndpoints.NannyHealth;
import com.jivesoftware.os.upena.deployable.UpenaEndpoints.NodeHealth;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.DiscoveredRoutes;
import com.jivesoftware.os.upena.service.DiscoveredRoutes.Route;
import com.jivesoftware.os.upena.service.DiscoveredRoutes.RouteHealths;
import com.jivesoftware.os.upena.service.DiscoveredRoutes.Routes;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang.exception.ExceptionUtils;

/**
 *
 */
// soy.page.healthPluginRegion
public class TopologyPluginRegion implements PageRegion<Optional<TopologyPluginRegion.TopologyPluginRegionInput>> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance();

    private final String template;
    private final String connectionHealthTemplate;
    private final SoyRenderer renderer;
    private final AmzaInstance amzaInstance;
    private final UpenaStore upenaStore;
    private final HealthPluginRegion healthPluginRegion;
    private final DiscoveredRoutes discoveredRoutes;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public TopologyPluginRegion(String template,
        String connectionHealthTemplate,
        SoyRenderer renderer,
        AmzaInstance amzaInstance,
        UpenaStore upenaStore,
        HealthPluginRegion healthPluginRegion,
        DiscoveredRoutes discoveredRoutes) {

        this.template = template;
        this.connectionHealthTemplate = connectionHealthTemplate;
        this.renderer = renderer;
        this.amzaInstance = amzaInstance;
        this.upenaStore = upenaStore;
        this.healthPluginRegion = healthPluginRegion;
        this.discoveredRoutes = discoveredRoutes;
    }

    public static class TopologyPluginRegionInput {

        final String cluster;
        final String host;
        final String service;
        final Set<String> graphType;

        public TopologyPluginRegionInput(String cluster, String host, String service, Set<String> graphType) {
            this.cluster = cluster;
            this.host = host;
            this.service = service;
            this.graphType = graphType;
        }
    }

    @Override
    public String render(String user, Optional<TopologyPluginRegionInput> optionalInput) {
        Map<String, Object> data = Maps.newHashMap();
        try {
            TopologyPluginRegionInput input = optionalInput.get();

            Map<String, Object> filter = new HashMap<>();
            filter.put("cluster", input.cluster);
            filter.put("host", input.host);
            filter.put("service", input.service);

            Map<String, Boolean> selectedTypes = new HashMap<>();
            for (String type : input.graphType) {
                selectedTypes.put(type, Boolean.TRUE);
            }
            filter.put("graphType", selectedTypes);
            data.put("filter", filter);

            if (input.graphType.contains("connectivity")) {
                connectivityGraph(data);
            }

            if (input.graphType.contains("topology")) {
                topologyGraph(data);
            }

            for (String ensure : new String[]{"connectivityNodes", "connectivityEdges", "topologyNodes", "topologyEdges"}) {
                if (!data.containsKey(ensure)) {
                    data.put(ensure, Collections.emptyList());
                }
            }

            return renderer.render(template, data);

        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
            return "Oops:" + ExceptionUtils.getStackTrace(e);
        }

    }

    private void topologyGraph(Map<String, Object> data) throws Exception, JsonProcessingException {
        int id = 0;
        buildClusterRoutes();
        Map<String, Map<HostPort, Map<String, ConnectionHealth>>> routes = discoveredRoutes.instanceHostPortFamilyConnectionHealths;

        Map<String, Edge> edges = new HashMap<>();
        Map<String, Node> nodes = new HashMap<>();

        InstanceFilter filter = new InstanceFilter(
            null,
            null,
            null,
            null,
            null,
            0, 10000);

        Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
        for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
            TimestampedValue<Instance> timestampedValue = entrySet.getValue();
            if (!timestampedValue.getTombstoned()) {
                Instance value = timestampedValue.getValue();
                Cluster cluster = upenaStore.clusters.get(value.clusterKey);
                Host host = upenaStore.hosts.get(value.hostKey);
                Service service = upenaStore.services.get(value.serviceKey);
                ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(value.releaseGroupKey);

                Node from = nodes.get(value.clusterKey.toString());
                if (from == null) {
                    from = new Node(cluster.name, id, "ccc", "12", 0);
                    id++;
                    nodes.put(value.clusterKey.toString(), from);
                    from.focusHtml = "";
                }

                Node to = nodes.get(value.hostKey.toString());
                if (to == null) {
                    to = new Node(host.name, id, "aaa", "12", 0);
                    id++;
                    nodes.put(value.hostKey.toString(), to);
                    to.focusHtml = "";
                }
                Edge edge = addEdge(edges, from, to);
                from = to;

                to = nodes.get(value.releaseGroupKey.toString());
                if (to == null) {
                    to = new Node(releaseGroup.name, id, "888", "12", 0);
                    id++;
                    nodes.put(value.releaseGroupKey.toString(), to);
                    to.focusHtml = "";
                }
                edge = addEdge(edges, from, to);
                from = to;

                to = nodes.get(value.serviceKey.toString());
                if (to == null) {
                    to = new Node(service.name, id, "666", "12", 0);
                    id++;
                    nodes.put(value.serviceKey.toString(), to);
                    to.focusHtml = "";
                }
                edge = addEdge(edges, from, to);
                from = to;

                to = nodes.get(entrySet.getKey().toString());
                if (to == null) {
                    to = new Node(""+value.instanceId, id, "444", "12", 0);
                    id++;
                    nodes.put(entrySet.getKey().toString(), to);
                    to.focusHtml = "";
                }
                edge = addEdge(edges, from, to);
            }
        }

        List<Map<String, String>> renderNodes = new ArrayList<>();
        for (Node n : nodes.values()) {
            Map<String, String> node = new HashMap<>();
            node.put("id", "id" + n.id);

            node.put("maxbgcolor", "888");
            node.put("minbgcolor", "888");
            node.put("fontSize", n.fontSize);
            node.put("label", n.label);
            node.put("count", String.valueOf(n.count));
            node.put("focusHtml", n.focusHtml);
            node.put("color", n.bgcolor);

            renderNodes.add(node);
        }

        data.put("topologyNodes", MAPPER.writeValueAsString(renderNodes));

        List<Map<String, String>> renderEdges = new ArrayList<>();
        for (Edge e : edges.values()) {
            Map<String, String> edge = new HashMap<>();
            edge.put("from", "id" + e.from);
            edge.put("label", e.label);
            edge.put("to", "id" + e.to);
            edge.put("color", "888");
            edge.put("minColor", "888");
            edge.put("maxColor", "888");
            renderEdges.add(edge);
        }

        data.put("topologyEdges", MAPPER.writeValueAsString(renderEdges));

    }

    private void connectivityGraph(Map<String, Object> data) throws Exception, JsonProcessingException {
        Map<String, Node> nodes = new HashMap<>();
        int id = 0;
        buildClusterRoutes();
        Map<String, Map<HostPort, Map<String, ConnectionHealth>>> routes = discoveredRoutes.instanceHostPortFamilyConnectionHealths;

        for (Map.Entry<String, Map<HostPort, Map<String, ConnectionHealth>>> entrySet : routes.entrySet()) {
            String instanceId = entrySet.getKey();
            Instance instance = upenaStore.instances.get(new InstanceKey(instanceId));
            Service service = null;
            if (instance != null) {
                service = upenaStore.services.get(instance.serviceKey);
            }
            String serviceName = service != null ? service.name : instanceId;
            Node from = nodes.get(serviceName);
            if (from == null) {
                from = new Node(serviceName, id, "666", "12", 0);
                id++;
                nodes.put(serviceName, from);

                double serviceHealth = serviceHealth(instanceId);
                from.maxHealth = Math.max(from.maxHealth, serviceHealth);
                from.minHealth = Math.min(from.minHealth, serviceHealth);
            } else {
                from.count++;
            }

            Map<HostPort, Map<String, ConnectionHealth>> hostPortFamilyConnectionHealths = entrySet.getValue();
            for (Map.Entry<HostPort, Map<String, ConnectionHealth>> hostPortFamilyConnectionHealth : hostPortFamilyConnectionHealths.entrySet()) {
                Map<String, ConnectionHealth> familyConnectionHealths = hostPortFamilyConnectionHealth.getValue();
                for (Map.Entry<String, ConnectionHealth> familyConnectionHealth : familyConnectionHealths.entrySet()) {
                    ConnectionHealth connectionHealth = familyConnectionHealth.getValue();
                    String toServiceName = connectionHealth.connectionDescriptor.getInstanceDescriptor().serviceName;
                    Node to = nodes.get(toServiceName);
                    if (to == null) {
                        to = new Node(toServiceName, id, "060", "12", 0);
                        id++;
                        nodes.put(toServiceName, to);

                        double serviceHealth = serviceHealth(connectionHealth.connectionDescriptor.getInstanceDescriptor().instanceKey);
                        to.maxHealth = Math.max(to.maxHealth, serviceHealth);
                        to.minHealth = Math.min(to.minHealth, serviceHealth);
                    }
                }
            }
        }

        Map<String, Edge> edges = new HashMap<>();

        for (Map.Entry<String, Map<HostPort, Map<String, ConnectionHealth>>> entrySet : routes.entrySet()) {
            String instanceId = entrySet.getKey();
            Map<HostPort, Map<String, ConnectionHealth>> hostPortFamilyConnectionHealths = entrySet.getValue();

            Instance instance = upenaStore.instances.get(new InstanceKey(instanceId));
            Service service = null;
            if (instance != null) {
                service = upenaStore.services.get(instance.serviceKey);
            }
            String serviceName = service != null ? service.name : instanceId;
            Node from = nodes.get(serviceName);

            MinMaxDouble mmd = new MinMaxDouble();
            from.focusHtml = renderConnectionHealth(mmd, nodes, serviceName, instanceId);

            for (Map.Entry<HostPort, Map<String, ConnectionHealth>> hostPortFamilyConnectionHealth : hostPortFamilyConnectionHealths.entrySet()) {

                HostPort hostPort = hostPortFamilyConnectionHealth.getKey();
                Map<String, ConnectionHealth> familyConnectionHealths = hostPortFamilyConnectionHealth.getValue();
                Node to = null;
                MinMaxDouble edgeWeight = new MinMaxDouble();
                double successPerSecond = 0;
                for (Map.Entry<String, ConnectionHealth> familyConnectionHealth : familyConnectionHealths.entrySet()) {
                    String family = familyConnectionHealth.getKey();
                    ConnectionHealth connectionHealth = familyConnectionHealth.getValue();

                    if (to == null) {
                        String toServiceName = connectionHealth.connectionDescriptor.getInstanceDescriptor().serviceName;
                        to = nodes.get(toServiceName);
                        if (to == null) {
                            to = new Node(toServiceName, id, "060", "12", 0);
                            nodes.put(toServiceName, to);
                            id++;
                        }

                    }
                    successPerSecond += connectionHealth.successPerSecond;

                    edgeWeight.value(connectionHealth.latencyStats.latency90th);
                    edgeWeight.value(connectionHealth.latencyStats.latency95th);
                    edgeWeight.value(connectionHealth.latencyStats.latency99th);
                }

                Edge edge = addEdge(edges, from, to);
                edge.min = 1d - mmd.zeroToOne(edgeWeight.min);
                edge.max = 1d - mmd.zeroToOne(edgeWeight.max);
                edge.label = numberFormat.format(successPerSecond) + "/sec";
            }
        }

        List<Map<String, String>> renderNodes = new ArrayList<>();
        for (Node n : nodes.values()) {
            Map<String, String> node = new HashMap<>();
            node.put("id", "id" + n.id);

            if (n.maxHealth == Double.MAX_VALUE) {
                node.put("maxbgcolor", n.bgcolor);
                node.put("minbgcolor", n.bgcolor);
            } else {
                node.put("maxbgcolor", healthPluginRegion.getHEXTrafficlightColor(n.maxHealth, 1f));
                node.put("minbgcolor", healthPluginRegion.getHEXTrafficlightColor(n.minHealth, 1f));
            }
            node.put("fontSize", n.fontSize);
            node.put("label", n.label + " (" + n.count + ")");
            node.put("count", String.valueOf(n.count));
            node.put("focusHtml", n.focusHtml);
            node.put("color", healthPluginRegion.getHEXIdColor(((float) n.id / (float) id), 1f));

            renderNodes.add(node);
        }

        data.put("connectivityNodes", MAPPER.writeValueAsString(renderNodes));

        List<Map<String, String>> renderEdges = new ArrayList<>();
        for (Edge e : edges.values()) {
            Map<String, String> edge = new HashMap<>();
            edge.put("from", "id" + e.from);
            edge.put("label", e.label);
            edge.put("to", "id" + e.to);
            edge.put("color", healthPluginRegion.getHEXIdColor(((float) e.from / (float) id), 1f));
            edge.put("minColor", healthPluginRegion.getHEXTrafficlightColor(e.min, 1f));
            edge.put("maxColor", healthPluginRegion.getHEXTrafficlightColor(e.max, 1f));
            renderEdges.add(edge);
        }

        data.put("connectivityEdges", MAPPER.writeValueAsString(renderEdges));

    }

    private double serviceHealth(String instanceId) throws Exception {
        NannyHealth health = null;
        Collection<NodeHealth> nodeHealths = healthPluginRegion.buildClusterHealth();
        for (NodeHealth nodeHealth : nodeHealths) {
            for (NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                if (nannyHealth.instanceDescriptor.instanceKey.equals(instanceId)) {
                    health = nannyHealth;
                    break;
                }
            }
        }
        double serviceHealth = health == null ? 0d : Math.max(0d, Math.min(health.serviceHealth.health, 1d));
        return serviceHealth;
    }

    private String renderConnectionHealth(MinMaxDouble mmd, Map<String, Node> nodes, String from, String instanceId) throws Exception {
        List<Map<String, Object>> healths = new ArrayList<>();
        Map<HostPort, Map<String, ConnectionHealth>> connectionHealths = discoveredRoutes.getConnectionHealth(instanceId);

        for (Map.Entry<HostPort, Map<String, ConnectionHealth>> hostPortHealth : connectionHealths.entrySet()) {

            for (Map.Entry<String, ConnectionHealth> familyHealth : hostPortHealth.getValue().entrySet()) {

                mmd.value(familyHealth.getValue().latencyStats.latencyMin);
                mmd.value(familyHealth.getValue().latencyStats.latencyMean);
                mmd.value(familyHealth.getValue().latencyStats.latencyMax);

                mmd.value(familyHealth.getValue().latencyStats.latency50th);
                mmd.value(familyHealth.getValue().latencyStats.latency75th);
                mmd.value(familyHealth.getValue().latencyStats.latency90th);
                mmd.value(familyHealth.getValue().latencyStats.latency95th);
                mmd.value(familyHealth.getValue().latencyStats.latency99th);
                mmd.value(familyHealth.getValue().latencyStats.latency999th);
            }
        }

        for (Map.Entry<HostPort, Map<String, ConnectionHealth>> hostPortHealth : connectionHealths.entrySet()) {

            for (Map.Entry<String, ConnectionHealth> familyHealth : hostPortHealth.getValue().entrySet()) {

                ConnectionHealth value = familyHealth.getValue();

                Map<String, Object> health = new HashMap<>();
                health.put("from", from);
                Node fromNode = nodes.get(from);
                health.put("fromColor", healthPluginRegion.idColorRGB(((float) fromNode.id / (float) nodes.size()), 1f));

                health.put("to", value.connectionDescriptor.getInstanceDescriptor().serviceName);
                Node toNode = nodes.get(value.connectionDescriptor.getInstanceDescriptor().serviceName);
                health.put("toColor", healthPluginRegion.idColorRGB(((float) toNode.id / (float) nodes.size()), 1f));

                health.put("family", familyHealth.getKey());

                health.put("success", numberFormat.format(value.success));
                health.put("successPerSecond", numberFormat.format(value.successPerSecond));

                health.put("inflight", numberFormat.format(value.attempt - value.success));

                health.put("min", numberFormat.format(value.latencyStats.latencyMin));
                health.put("minColor", healthPluginRegion.trafficlightColorRGB(1d - mmd.zeroToOne(value.latencyStats.latencyMin), 1f));
                health.put("mean", numberFormat.format(value.latencyStats.latencyMean));
                health.put("meanColor", healthPluginRegion.trafficlightColorRGB(1d - mmd.zeroToOne(value.latencyStats.latencyMean), 1f));
                health.put("max", numberFormat.format(value.latencyStats.latencyMax));
                health.put("maxColor", healthPluginRegion.trafficlightColorRGB(1d - mmd.zeroToOne(value.latencyStats.latencyMax), 1f));

                health.put("latency50th", numberFormat.format(value.latencyStats.latency50th));
                health.put("latency50thColor", healthPluginRegion.trafficlightColorRGB(1d - mmd.zeroToOne(value.latencyStats.latency50th), 1f));
                health.put("latency75th", numberFormat.format(value.latencyStats.latency75th));
                health.put("latency75thColor", healthPluginRegion.trafficlightColorRGB(1d - mmd.zeroToOne(value.latencyStats.latency75th), 1f));
                health.put("latency90th", numberFormat.format(value.latencyStats.latency90th));
                health.put("latency90thColor", healthPluginRegion.trafficlightColorRGB(1d - mmd.zeroToOne(value.latencyStats.latency90th), 1f));
                health.put("latency95th", numberFormat.format(value.latencyStats.latency95th));
                health.put("latency95thColor", healthPluginRegion.trafficlightColorRGB(1d - mmd.zeroToOne(value.latencyStats.latency95th), 1f));
                health.put("latency99th", numberFormat.format(value.latencyStats.latency99th));
                health.put("latency99thColor", healthPluginRegion.trafficlightColorRGB(1d - mmd.zeroToOne(value.latencyStats.latency99th), 1f));
                health.put("latency999th", numberFormat.format(value.latencyStats.latency999th));
                health.put("latency999thColor", healthPluginRegion.trafficlightColorRGB(1d - mmd.zeroToOne(value.latencyStats.latency999th), 1f));

                health.put("host", hostPortHealth.getKey().getHost());
                health.put("port", hostPortHealth.getKey().getPort());

                healths.add(health);
            }
        }

        Collections.sort(healths, (Map<String, Object> o1, Map<String, Object> o2) -> {

            return ((String) o1.get("family")).compareTo((String) o2.get("family"));
        });

        Map<String, Object> data = new HashMap<>();
        data.put("healths", healths);
        return renderer.render(connectionHealthTemplate, data);
    }

    private Edge addEdge(Map<String, Edge> edges, Node from, Node to) {
        Edge edge = edges.get(from.id + "->" + to.id);
        if (edge == null) {
            edge = new Edge(from.id, to.id, "");
            edges.put(from.id + "->" + to.id, edge);
            System.out.println("edge:" + edge);
        }
        return edge;
    }

    public static class Node {

        String label;
        int id;
        String bgcolor;
        String fontSize;
        String focusHtml;
        int count = 1;
        double maxHealth = -Double.MAX_VALUE;
        double minHealth = Double.MAX_VALUE;

        public Node(String label, int id, String bgcolor, String fontSize, int count) {
            this.label = label;
            this.id = id;
            this.bgcolor = bgcolor;
            this.fontSize = fontSize;
            this.count = count;
        }

        @Override
        public String toString() {
            return "Node{" + "label=" + label + ", id=" + id + ", bgcolor=" + bgcolor + ", count=" + count + '}';
        }

    }

    public static class Edge {

        int from;
        String label;
        double min;
        double max;
        int to;
        String edgeColor = "000";

        public Edge(int from, int to, String label) {
            this.from = from;
            this.to = to;
            this.label = label;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + this.from;
            hash = 79 * hash + this.to;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Edge other = (Edge) obj;
            if (this.from != other.from) {
                return false;
            }
            if (this.to != other.to) {
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

    public void toInstance(HostPort hostPort) {

    }

    @Override
    public String getTitle() {
        return "Topology";
    }

    private final ConcurrentMap<RingHost, Routes> nodeRoutes = Maps.newConcurrentMap();
    private final ConcurrentMap<String, Long> nodeRecency = Maps.newConcurrentMap();
    private final ConcurrentMap<RingHost, Boolean> currentlyExecuting = Maps.newConcurrentMap();

    private List<Route> buildClusterRoutes() throws Exception {
        List<Route> allRoutes = new ArrayList<>();

        allRoutes.addAll(discoveredRoutes.routes());
        for (Routes v : nodeRoutes.values()) {
            allRoutes.addAll(v.getRoutes());
        }

//        for (RingHost ringHost : new RingHost[]{
//            new RingHost("soa-prime-data5.phx1.jivehosted.com", 1175),
//            new RingHost("soa-prime-data6.phx1.jivehosted.com", 1175),
//            new RingHost("soa-prime-data7.phx1.jivehosted.com", 1175),
//            new RingHost("soa-prime-data8.phx1.jivehosted.com", 1175),
//            new RingHost("soa-prime-data9.phx1.jivehosted.com", 1175),
//            new RingHost("soa-prime-data10.phx1.jivehosted.com", 1175)
//        }) {
        for (final RingHost ringHost : amzaInstance.getRing("MASTER")) {
            if (currentlyExecuting.putIfAbsent(ringHost, true) == null) {
                executorService.submit(() -> {
                    long start = System.currentTimeMillis();
                    String nodeKey = ringHost.getHost() + ":" + ringHost.getPort();

                    try {
                        Long last = nodeRecency.get(nodeKey);
                        long sinceTimestampMillis = last == null ? 0 : last;
                        HttpRequestHelper requestHelper = buildRequestHelper(ringHost.getHost(), ringHost.getPort());
                        RouteHealths routeHealths = requestHelper.executeGetRequest("/routes/health/" + sinceTimestampMillis, RouteHealths.class, null);
                        for (InstanceConnectionHealth routeHealth : routeHealths.getRouteHealths()) {
                            discoveredRoutes.connectionHealth(routeHealth);
                        }
                    } catch (Exception x) {
                        System.out.println("Failed getting route health for instances " + ringHost + " " + x);
                    }

                    try {
                        HttpRequestHelper requestHelper = buildRequestHelper(ringHost.getHost(), ringHost.getPort());
                        Routes routes = requestHelper.executeGetRequest("/routes/instances", Routes.class, null);
                        nodeRoutes.put(ringHost, routes);
                    } catch (Exception x) {
                        Routes routes = new Routes(Collections.emptyList());
                        nodeRoutes.put(ringHost, routes);
                        System.out.println("Failed getting routes for instances " + ringHost + " " + x);
                    } finally {
                        nodeRecency.put(nodeKey, start);
                        currentlyExecuting.remove(ringHost);
                    }

                });
            }
        }
        return allRoutes;
    }

    HttpRequestHelper buildRequestHelper(String host, int port) {
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().setSocketTimeoutInMillis(10000).build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider()
            .createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig));
        HttpClient httpClient = httpClientFactory.createClient(host, port);
        HttpRequestHelper requestHelper = new HttpRequestHelper(httpClient, new ObjectMapper());
        return requestHelper;
    }

}
