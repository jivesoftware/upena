package com.jivesoftware.os.upena.deployable.region;

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
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.HostPort;
import com.jivesoftware.os.upena.deployable.UpenaEndpoints.NannyHealth;
import com.jivesoftware.os.upena.deployable.UpenaEndpoints.NodeHealth;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.DiscoveredRoutes;
import com.jivesoftware.os.upena.service.DiscoveredRoutes.Route;
import com.jivesoftware.os.upena.service.DiscoveredRoutes.RouteHealths;
import com.jivesoftware.os.upena.service.DiscoveredRoutes.Routes;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.ConnectionHealth;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
// soy.page.healthPluginRegion
public class TopologyPluginRegion implements PageRegion<Optional<TopologyPluginRegion.TopologyPluginRegionInput>> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String template;
    private final SoyRenderer renderer;
    private final AmzaInstance amzaInstance;
    private final UpenaStore upenaStore;
    private final HealthPluginRegion healthPluginRegion;
    private final DiscoveredRoutes discoveredRoutes;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public TopologyPluginRegion(String template,
        SoyRenderer renderer,
        AmzaInstance amzaInstance,
        UpenaStore upenaStore,
        HealthPluginRegion healthPluginRegion,
        DiscoveredRoutes discoveredRoutes) {

        this.template = template;
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

        public TopologyPluginRegionInput(String cluster, String host, String service) {
            this.cluster = cluster;
            this.host = host;
            this.service = service;
        }
    }

    @Override
    public String render(String user, Optional<TopologyPluginRegionInput> optionalInput) {
        Map<String, Object> data = Maps.newHashMap();
        boolean portNames = false;
        try {
            TopologyPluginRegionInput input = optionalInput.get();

            Map<String, String> filter = new HashMap<>();
            filter.put("cluster", input.cluster);
            filter.put("host", input.host);
            filter.put("service", input.service);
            data.put("filter", filter);

            Map<String, Node> nodes = new HashMap<>();
            Map<String, Edge> edges = new HashMap<>();
            int id = 0;
            for (Route route : buildClusterRoutes()) {

                Instance instance = upenaStore.instances.get(new InstanceKey(route.getInstanceId()));

                NannyHealth health = null;
                Collection<NodeHealth> nodeHealths = healthPluginRegion.buildClusterHealth();
                for (NodeHealth nodeHealth : nodeHealths) {
                    for (NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                        if (nannyHealth.instanceDescriptor.instanceKey.equals(route.getInstanceId())) {
                            health = nannyHealth;
                            break;
                        }
                    }
                }
                double serviceHealth = health == null ? 0d : Math.max(0d, Math.min(health.serviceHealth.health, 1d));

                Service service = null;
                if (instance != null) {
                    service = upenaStore.services.get(instance.serviceKey);
                }
                String serviceName = service != null ? service.name : route.getInstanceId();
                Node from = nodes.get(serviceName);
                if (from == null) {
                    from = new Node(serviceName, "id" + id, "666", "16", 0);
                    nodes.put(serviceName, from);
                    id++;
                    System.out.println("from:" + from);
                }
                from.maxHealth = Math.max(from.maxHealth, serviceHealth);
                from.minHealth = Math.min(from.minHealth, serviceHealth);
                from.count++;

                Node to;
                if (route.getReturnCode() == 1) {

                    if (portNames) {
                        to = nodes.get(route.getPortName() + "->" + route.getConnectToServiceNamed());
                        if (to == null) {
                            to = new Node(route.getPortName(), "id" + id, "004", "10", 0);
                            nodes.put(route.getPortName() + "->" + route.getConnectToServiceNamed(), to);
                            id++;
                            System.out.println("to:" + to);
                        }
                        to.count++;

                        addEdge(edges, from, to);
                        from = to;
                    }

                    to = nodes.get(route.getConnectToServiceNamed());
                    if (to == null) {
                        to = new Node(route.getConnectToServiceNamed(), "id" + id, "060", "16", 0);
                        nodes.put(route.getConnectToServiceNamed(), to);
                        id++;
                        System.out.println("to:" + to);
                    }
                    to.maxHealth = Math.max(to.maxHealth, serviceHealth);
                    to.minHealth = Math.min(to.minHealth, serviceHealth);
                    to.count++;

                } else {
                    to = nodes.get("Errors");
                    if (to == null) {
                        to = new Node("Errors", "id" + id, "900", "20", 0);
                        nodes.put("Errors", to);
                        id++;
                        System.out.println("to:" + to);
                    }
                    to.count++;
                }

                Edge edge = addEdge(edges, from, to);

                long successes = 0;
                for (ConnectionDescriptor connection : route.getConnections()) {
                    Map<String, ConnectionHealth> connectionHealth = discoveredRoutes.getConnectionHealth(connection.getHostPort());
                    for (Map.Entry<String, ConnectionHealth> entrySet : connectionHealth.entrySet()) {
                        successes += entrySet.getValue().success;
                    }
                }
                if (successes > 0) {
                    edge.label = "(" + successes + ")";
                } else {
                    edge.label = "";
                }

            }

//            edges.add(new Edge("a", "b"));
//            edges.add(new Edge("a", "c"));
//            edges.add(new Edge("c", "d"));
//            edges.add(new Edge("c", "d"));
//            edges.add(new Edge("b", "e"));
//            edges.add(new Edge("b", "f"));
            List<Map<String, String>> renderNodes = new ArrayList<>();
            for (Node n : nodes.values()) {
                Map<String, String> node = new HashMap<>();
                node.put("id", n.id);

                if (n.maxHealth == Double.MAX_VALUE) {
                    node.put("maxbgcolor", n.bgcolor);
                    node.put("minbgcolor", n.bgcolor);
                } else {
                    node.put("maxbgcolor", healthPluginRegion.getHEXTrafficlightColor(n.maxHealth, 1f));
                    node.put("minbgcolor", healthPluginRegion.getHEXTrafficlightColor(n.minHealth, 1f));
                }
                node.put("fontSize", n.fontSize);
                node.put("label", n.label + "\n(" + n.count + ")");
                node.put("count", String.valueOf(n.count));
                renderNodes.add(node);
            }

            data.put("nodes", MAPPER.writeValueAsString(renderNodes));

            List<Map<String, String>> renderEdges = new ArrayList<>();
            for (Edge e : edges.values()) {
                Map<String, String> edge = new HashMap<>();
                edge.put("from", e.from);
                edge.put("label", e.label);
                edge.put("to", e.to);
                renderEdges.add(edge);
            }

            data.put("edges", MAPPER.writeValueAsString(renderEdges));

        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
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
        String id;
        String bgcolor;
        String fontSize;
        int count;
        double maxHealth = -Double.MAX_VALUE;
        double minHealth = Double.MAX_VALUE;

        public Node(String label, String id, String bgcolor, String fontSize, int count) {
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

        String from;
        String label;
        String to;

        public Edge(String from, String to, String label) {
            this.from = from;
            this.to = to;
            this.label = label;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.from);
            hash = 97 * hash + Objects.hashCode(this.to);
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
                        for (ConnectionHealth routeHealth : routeHealths.getRouteHealths()) {
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
