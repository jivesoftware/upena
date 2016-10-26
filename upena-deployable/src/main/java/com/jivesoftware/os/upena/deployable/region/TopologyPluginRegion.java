package com.jivesoftware.os.upena.deployable.region;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelperUtils;
import com.jivesoftware.os.routing.bird.shared.HostPort;
import com.jivesoftware.os.routing.bird.shared.InstanceConnectionHealth;
import com.jivesoftware.os.upena.amza.shared.AmzaInstance;
import com.jivesoftware.os.upena.amza.shared.RingHost;
import com.jivesoftware.os.upena.deployable.UpenaEndpoints.NannyHealth;
import com.jivesoftware.os.upena.deployable.UpenaEndpoints.NodeHealth;
import com.jivesoftware.os.upena.deployable.region.ReleasesPluginRegion.ReleasesPluginRegionInput;
import com.jivesoftware.os.upena.deployable.region.TopologyPluginRegion.TopologyPluginRegionInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.DiscoveredRoutes;
import com.jivesoftware.os.upena.service.DiscoveredRoutes.Route;
import com.jivesoftware.os.upena.service.DiscoveredRoutes.RouteHealths;
import com.jivesoftware.os.upena.service.DiscoveredRoutes.Routes;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceFilter;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang.exception.ExceptionUtils;

/**
 *
 */
// soy.page.healthPluginRegion
public class TopologyPluginRegion implements PageRegion<TopologyPluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final ObjectMapper mapper;
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance();

    private final String template;
    private final String connectionHealthTemplate;
    private final SoyRenderer renderer;
    private final AmzaInstance amzaInstance;
    private final UpenaStore upenaStore;
    private final HealthPluginRegion healthPluginRegion;
    private final HostsPluginRegion hostsPluginRegion;
    private final ReleasesPluginRegion releasesPluginRegion;
    private final InstancesPluginRegion instancesPluginRegion;
    private final DiscoveredRoutes discoveredRoutes;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public TopologyPluginRegion(ObjectMapper mapper,
        String template,
        String connectionHealthTemplate,
        SoyRenderer renderer,
        AmzaInstance amzaInstance,
        UpenaStore upenaStore,
        HealthPluginRegion healthPluginRegion,
        HostsPluginRegion hostsPluginRegion,
        ReleasesPluginRegion releasesPluginRegion,
        InstancesPluginRegion instancesPluginRegion,
        DiscoveredRoutes discoveredRoutes) {

        this.mapper = mapper;
        this.template = template;
        this.connectionHealthTemplate = connectionHealthTemplate;
        this.renderer = renderer;
        this.amzaInstance = amzaInstance;
        this.upenaStore = upenaStore;
        this.healthPluginRegion = healthPluginRegion;
        this.hostsPluginRegion = hostsPluginRegion;
        this.releasesPluginRegion = releasesPluginRegion;
        this.instancesPluginRegion = instancesPluginRegion;
        this.discoveredRoutes = discoveredRoutes;
    }

    @Override
    public String getRootPath() {
        return "/ui/topology";
    }

    public static class TopologyPluginRegionInput implements PluginInput {

        final String clusterKey;
        final String cluster;
        final String hostKey;
        final String host;
        final String serviceKey;
        final String service;
        final String releaseKey;
        final String release;
        final Set<String> linkType;

        public TopologyPluginRegionInput(String clusterKey, String cluster, String hostKey, String host, String serviceKey, String service,
            String releaseKey, String release, Set<String> linkType) {
            this.clusterKey = clusterKey;
            this.cluster = cluster;
            this.hostKey = hostKey;
            this.host = host;
            this.serviceKey = serviceKey;
            this.service = service;
            this.releaseKey = releaseKey;
            this.release = release;
            this.linkType = linkType;
        }

        @Override
        public String name() {
            return "Topology";
        }

    }

    @Override
    public String render(String user, TopologyPluginRegionInput input) {
        Map<String, Object> data = Maps.newHashMap();
        try {

            Map<String, Object> filter = new HashMap<>();
            filter.put("clusterKey", input.clusterKey);
            filter.put("cluster", input.cluster);
            filter.put("hostKey", input.hostKey);
            filter.put("host", input.host);
            filter.put("serviceKey", input.serviceKey);
            filter.put("service", input.service);
            filter.put("releaseKey", input.releaseKey);
            filter.put("release", input.release);

            InstanceFilter instanceFilter = new InstanceFilter(
                input.clusterKey.isEmpty() ? null : new ClusterKey(input.clusterKey),
                input.hostKey.isEmpty() ? null : new HostKey(input.hostKey),
                input.serviceKey.isEmpty() ? null : new ServiceKey(input.serviceKey),
                input.releaseKey.isEmpty() ? null : new ReleaseGroupKey(input.releaseKey),
                null,
                0, 100_000);

            Map<String, Boolean> selectedLinkTypes = new HashMap<>();
            for (String type : input.linkType) {
                selectedLinkTypes.put(type, Boolean.TRUE);
            }
            filter.put("linkType", selectedLinkTypes);

            data.put("filters", filter);

            ConcurrentNavigableMap<ServiceKey, TimestampedValue<Service>> services = upenaStore.services.find(false, new ServiceFilter(null, null, 0, 100_000));

            Map<ServiceKey, TimestampedValue<com.jivesoftware.os.upena.shared.Service>> sort = new ConcurrentSkipListMap<>((ServiceKey o1, ServiceKey o2) -> {
                com.jivesoftware.os.upena.shared.Service so1 = services.get(o1).getValue();
                com.jivesoftware.os.upena.shared.Service so2 = services.get(o2).getValue();
                int c = so1.name.compareTo(so2.name);
                if (c != 0) {
                    return c;
                }
                return o1.compareTo(o2);
            });
            sort.putAll(services);

            int i = 0;
            Map<String, Integer> serviceColor = new HashMap<>();
            for (Map.Entry<ServiceKey, TimestampedValue<Service>> entrySet : sort.entrySet()) {
                if (!entrySet.getValue().getTombstoned()) {
                    serviceColor.put(entrySet.getValue().getValue().name, i);
                    i++;
                }
            }

            List<Map<String, String>> serviceNameLegend = new ArrayList<>();
            i = 0;
            for (Map.Entry<ServiceKey, TimestampedValue<Service>> entrySet : sort.entrySet()) {
                if (!entrySet.getValue().getTombstoned()) {
                    String idColor = healthPluginRegion.getHEXIdColor((double) i / (double) serviceColor.size(), 1f);
                    serviceNameLegend.add(ImmutableMap.of("name", entrySet.getValue().getValue().name, "color", idColor));
                    i++;
                }
            }

            data.put("serviceLegend", mapper.writeValueAsString(serviceNameLegend));

            topologyGraph(user, instanceFilter, input.linkType, serviceColor, data);

            for (String ensure : new String[]{"topologyNodes", "topologyEdges"}) {
                if (!data.containsKey(ensure)) {
                    data.put(ensure, Collections.emptyList());
                }
            }

            return renderer.render(template, data);

        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
            return "Oops:" + ExceptionUtils.getStackTrace(e);
        }

    }

    private void topologyGraph(String user, InstanceFilter filter, Set<String> linkType, Map<String, Integer> serviceColor, Map<String, Object> data) throws
        Exception,
        JsonProcessingException {

        int id = 0;
        buildClusterRoutes();

        Map<String, Edge> edges = new HashMap<>();
        Map<String, Node> nodes = new HashMap<>();

        Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(false, filter);
        for (Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
            TimestampedValue<Instance> timestampedValue = entrySet.getValue();
            if (!timestampedValue.getTombstoned()) {
                String instanceKey = entrySet.getKey().toString();
                Instance value = timestampedValue.getValue();
                Cluster cluster = upenaStore.clusters.get(value.clusterKey);
                Host host = upenaStore.hosts.get(value.hostKey);
                Service service = upenaStore.services.get(value.serviceKey);
                ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(value.releaseGroupKey);

                NannyHealth nannyHealth = nannyHealth(instanceKey);
                double serviceHealth = value.enabled ? serviceHealth(nannyHealth) : Double.MAX_VALUE;

                List<Node> linkable = new ArrayList<>();

                int fs = 22;
                if (linkType.contains("linkCluster")) {
                    Node n = nodes.get(value.clusterKey.toString());
                    if (n == null) {
                        n = new Node(cluster.name, id, "ccc", String.valueOf(fs), 0);
                        id++;
                        nodes.put(value.clusterKey.toString(), n);
                        String title = title(cluster, null, null, null, null);
                        n.focusHtml = title + "<br>" + healthPluginRegion.render("topology",
                            new HealthPluginRegion.HealthPluginRegionInput("", "", cluster.name, "", ""));
                        fs -= 2;

                        n.maxHealth = Math.max(n.maxHealth, serviceHealth);
                        n.minHealth = Math.min(n.minHealth, serviceHealth);

                        n.tooltip = cluster.description;
                        n.icon = "cluster";
                    }
                    linkable.add(n);
                }
                if (linkType.contains("linkRack")) {

                    Node n = nodes.get(host.rackName);
                    if (n == null) {
                        n = new Node(host.rackName, id, "ccc", String.valueOf(fs), 0);
                        id++;
                        nodes.put(host.rackName, n);
                        String title = title(cluster, null, null, null, null); //TODO fix to include rack
                        n.focusHtml = title + "<br>" + healthPluginRegion.render("topology",
                            new HealthPluginRegion.HealthPluginRegionInput("", "", cluster.name, "", "")); //TODO fix to include rack
                        //fs -= 2;

                        n.maxHealth = Math.max(n.maxHealth, serviceHealth);
                        n.minHealth = Math.min(n.minHealth, serviceHealth);

                        n.tooltip = host.rackName;
                        n.icon = "rack";
                    }
                    linkable.add(n);
                }
                if (linkType.contains("linkHost")) {
                    Node n = nodes.get(value.hostKey.toString());
                    if (n == null) {
                        n = new Node(null, id, "aaa", String.valueOf(fs), 0);
                        n.tooltip = host.hostName;
                        id++;
                        nodes.put(value.hostKey.toString(), n);
                        String title = title(cluster, host, null, null, null);
                        n.focusHtml = title + "<br>" + instancesPluginRegion.renderSimple(user, new InstancesPluginRegion.InstancesPluginRegionInput(
                            "", "", "", value.hostKey.getKey(), host.name, "", "", "", "", "", false, false, false, "", "", "filter"));

                        /*
                         n.focusHtml = hostsPluginRegion.render(user,
                         new HostsPluginRegion.HostsPluginRegionInput(value.hostKey.getKey(), host.name, host.hostName, "", "", "filter"));*/
                        fs -= 2;

                        n.maxHealth = Math.max(n.maxHealth, serviceHealth);
                        n.minHealth = Math.min(n.minHealth, serviceHealth);
                        n.icon = "host";
                    }
                    linkable.add(n);
                }

                String idColor = serviceIdColor(serviceColor, service.name);

                if (linkType.contains("linkService")) {
                    Node n = nodes.get(value.serviceKey.toString());
                    if (n == null) {
                        n = new Node(null, id, idColor, String.valueOf(fs), 0);
                        id++;
                        nodes.put(value.serviceKey.toString(), n);
                        String title = title(cluster, host, service, null, null);
                        n.focusHtml = title + "<br>" + instancesPluginRegion.renderSimple(user, new InstancesPluginRegion.InstancesPluginRegionInput(
                            "", "", "", "", "", value.serviceKey.toString(), service.name, "", "", "", false, false, false, "", "", "filter"));
                        fs -= 2;

                        n.maxHealth = Math.max(n.maxHealth, serviceHealth);
                        n.minHealth = Math.min(n.minHealth, serviceHealth);
                        n.tooltip = service.name;
                        n.icon = "service";
                    }
                    linkable.add(n);
                }

                String versions = "";
                for (String dep : releaseGroup.version.split(",")) {
                    String[] coord = dep.split(":");
                    versions += coord[3] + "\n";
                }

                if (linkType.contains("linkRelease")) {
                    Node n = nodes.get(value.releaseGroupKey.toString());
                    if (n == null) {

                        n = new Node(versions, id, idColor, String.valueOf(fs), 0);
                        id++;
                        nodes.put(value.releaseGroupKey.toString(), n);

                        String title = title(cluster, host, service, versions, null);
                        n.focusHtml = title + "<br>" + releasesPluginRegion.renderSimple(user,
                            new ReleasesPluginRegionInput(value.releaseGroupKey.toString(), releaseGroup.name, "", "", "", "", "", "", false, "filter"));
                        fs -= 2;

                        n.maxHealth = Math.max(n.maxHealth, serviceHealth);
                        n.minHealth = Math.min(n.minHealth, serviceHealth);

                        n.tooltip = releaseGroup.version;
                        n.icon = "release";
                    }
                    linkable.add(n);
                }
                if (linkType.contains("linkInstance")) {

                    Node n = nodes.get(instanceKey);
                    if (n == null) {
                        n = new Node(String.valueOf(value.instanceId), id, idColor, String.valueOf(fs), 0);
                        id++;
                        nodes.put(instanceKey, n);

                        String title = title(cluster, host, service, versions, entrySet);
                        n.focusHtml = title;
                        fs -= 2;

                        n.maxHealth = Math.max(n.maxHealth, serviceHealth);
                        n.minHealth = Math.min(n.minHealth, serviceHealth);
                        n.tooltip = service.name + "\n";
                        for (Map.Entry<String, Instance.Port> e : entrySet.getValue().getValue().ports.entrySet()) {
                            n.tooltip += e.getKey() + ":" + e.getValue().port + "\n";
                        }
                        n.icon = "instance";
                    }
                    linkable.add(n);

                }

                //System.out.println("linkable.size() = " + linkable.size());
                for (int i = 0; i < linkable.size() - 1; i++) {
                    Node f = linkable.get(i);
                    Node t = linkable.get(i + 1);
                    Edge e = addEdge(edges, f, t);
                    e.min = serviceHealth;
                    e.max = serviceHealth;

                }
            }
        }

        List<Map<String, String>> renderNodes = new ArrayList<>();
        for (Node n : nodes.values()) {
            Map<String, String> node = new HashMap<>();
            node.put("id", "id" + n.id);

            if (n.maxHealth == Double.MAX_VALUE) {
                node.put("maxbgcolor", n.bgcolor);
                node.put("minbgcolor", n.bgcolor);
                node.put("healthRadius", "0");
            } else {
                node.put("maxbgcolor", healthPluginRegion.getHEXTrafficlightColor(n.maxHealth, 1f));
                node.put("minbgcolor", healthPluginRegion.getHEXTrafficlightColor(n.minHealth, 1f));
                node.put("healthRadius", String.valueOf(6)); //String.valueOf((int) (1d - n.minHealth) * 4));
            }
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
            node.put("count", String.valueOf(n.count));
            node.put("focusHtml", n.focusHtml);
            node.put("color", n.bgcolor);

            renderNodes.add(node);
        }

        data.put("topologyNodes", mapper.writeValueAsString(renderNodes));

        List<Map<String, String>> renderEdges = new ArrayList<>();
        for (Edge e : edges.values()) {
            Map<String, String> edge = new HashMap<>();
            edge.put("from", "id" + e.from);
            edge.put("label", e.label);
            edge.put("to", "id" + e.to);
            edge.put("color", "888");
            edge.put("minColor", "888");
            edge.put("maxColor", healthPluginRegion.getHEXTrafficlightColor(e.min, 1f));
            renderEdges.add(edge);
        }

        data.put("topologyEdges", mapper.writeValueAsString(renderEdges));

    }

    private String title(Cluster cluster, Host host, Service service, String versions, Map.Entry<InstanceKey, TimestampedValue<Instance>> entrySet) {
        String title = "<label>";
        if (cluster != null) {
            title += "<img src=\"/static/img/cluster.png\" alt=\"Cluster\" style=\"width:24px;height:24px;\">&nbsp";
            title += cluster.name + "&nbsp&nbsp&nbsp";
        }
        if (host != null) {
            title += "<img src=\"/static/img/host.png\" alt=\"Host\" style=\"width:24px;height:24px;\">&nbsp";
            title += host.name + "&nbsp&nbsp&nbsp";
        }
        if (service != null) {
            title += "<img src=\"/static/img/service.png\" alt=\"Service\" style=\"width:24px;height:24px;\">&nbsp";
            title += service.name + "&nbsp&nbsp&nbsp";
        }
        if (versions != null) {
            title += "<img src=\"/static/img/release.png\" alt=\"Release\" style=\"width:24px;height:24px;\">&nbsp";
            title += versions + "&nbsp&nbsp&nbsp";
        }
        if (entrySet != null) {
            title += "<img src=\"/static/img/instance.png\" alt=\"Instance\" style=\"width:24px;height:24px;\">&nbsp";
            title += entrySet.getValue().getValue().instanceId + "&nbsp&nbsp&nbsp";
        }
        title += "</lable>";
        return title;
    }

    private String serviceIdColor(Map<String, Integer> serviceColor, String serviceName) {
        Integer si = serviceColor.get(serviceName);
        if (si == null) {
            si = 0;
        }
        String idColor = healthPluginRegion.getHEXIdColor(((float) si / (float) serviceColor.size()), 1f);
        return idColor;
    }

    private NannyHealth nannyHealth(String instanceId) throws Exception {
        NannyHealth health = null;
        Collection<NodeHealth> nodeHealths = healthPluginRegion.buildClusterHealth().values();
        for (NodeHealth nodeHealth : nodeHealths) {
            for (NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                if (nannyHealth.instanceDescriptor.instanceKey.equals(instanceId)) {
                    health = nannyHealth;
                    break;
                }
            }
        }
        return health;
    }

    private double serviceHealth(NannyHealth health) {
        return health == null ? 0d : Math.max(0d, Math.min(health.serviceHealth.health, 1d));
    }

    private Edge addEdge(Map<String, Edge> edges, Node from, Node to) {
        Edge edge = edges.get(from.id + "->" + to.id);
        if (edge == null) {
            edge = new Edge(from.id, to.id, "");
            edges.put(from.id + "->" + to.id, edge);
            //System.out.println("edge:" + edge);
        }
        return edge;
    }

    public static class Node {

        String label;
        String icon;
        int id;
        String bgcolor;
        String fontSize;
        String focusHtml;
        String tooltip;
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
                        HttpRequestHelper requestHelper = HttpRequestHelperUtils.buildRequestHelper(false, false, null, ringHost.getHost(), ringHost.getPort());
                        RouteHealths routeHealths = requestHelper.executeGetRequest("/routes/health/" + sinceTimestampMillis, RouteHealths.class, null);
                        for (InstanceConnectionHealth routeHealth : routeHealths.getRouteHealths()) {
                            discoveredRoutes.connectionHealth(routeHealth);
                        }
                    } catch (Exception x) {
                        LOG.warn("Failed getting route health for instances {} {}", ringHost, x);
                    }

                    try {
                        HttpRequestHelper requestHelper = HttpRequestHelperUtils.buildRequestHelper(false, false, null, ringHost.getHost(), ringHost.getPort());
                        Routes routes = requestHelper.executeGetRequest("/routes/instances", Routes.class, null);
                        nodeRoutes.put(ringHost, routes);
                    } catch (Exception x) {
                        Routes routes = new Routes(Collections.emptyList());
                        nodeRoutes.put(ringHost, routes);
                        LOG.warn("Failed getting routes for instances {} {}", ringHost, x);
                    } finally {
                        nodeRecency.put(nodeKey, start);
                        currentlyExecuting.remove(ringHost);
                    }

                });
            }
        }
        return allRoutes;
    }

}
