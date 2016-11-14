package com.jivesoftware.os.upena.deployable.region;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelperUtils;
import com.jivesoftware.os.routing.bird.shared.ConnectionHealth;
import com.jivesoftware.os.routing.bird.shared.InstanceConnectionHealth;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptor;
import com.jivesoftware.os.upena.amza.shared.AmzaInstance;
import com.jivesoftware.os.upena.amza.shared.RingHost;
import com.jivesoftware.os.upena.deployable.UpenaHealth;
import com.jivesoftware.os.upena.deployable.UpenaHealth.NannyHealth;
import com.jivesoftware.os.upena.deployable.UpenaHealth.NodeHealth;
import com.jivesoftware.os.upena.deployable.UpenaSSLConfig;
import com.jivesoftware.os.upena.deployable.region.ConnectivityPluginRegion.ConnectivityPluginRegionInput;
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
import org.apache.commons.lang.exception.ExceptionUtils;

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
import java.util.concurrent.ThreadFactory;

/**
 *
 */
// soy.page.healthPluginRegion
public class ConnectivityPluginRegion implements PageRegion<ConnectivityPluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final ObjectMapper mapper;
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance();

    private final String template;
    private final String connectionHealthTemplate;
    private final String connectionOverviewTemplate;
    private final SoyRenderer renderer;
    private final UpenaHealth upenaHealth;
    private final AmzaInstance amzaInstance;
    private final UpenaSSLConfig upenaSSLConfig;
    private final UpenaStore upenaStore;
    private final HealthPluginRegion healthPluginRegion;
    private final DiscoveredRoutes discoveredRoutes;
    private final ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("discoveredRoutes-%d").build();
    private final ExecutorService executorService = Executors.newCachedThreadPool(namedThreadFactory);

    public ConnectivityPluginRegion(ObjectMapper mapper,
        String template,
        String connectionHealthTemplate,
        String connectionOverviewTemplate,
        SoyRenderer renderer,
        UpenaHealth upenaHealth,
        AmzaInstance amzaInstance,
        UpenaSSLConfig upenaSSLConfig,
        UpenaStore upenaStore,
        HealthPluginRegion healthPluginRegion,
        DiscoveredRoutes discoveredRoutes) {

        this.mapper = mapper;
        this.template = template;
        this.connectionHealthTemplate = connectionHealthTemplate;
        this.connectionOverviewTemplate = connectionOverviewTemplate;
        this.renderer = renderer;
        this.upenaHealth = upenaHealth;
        this.amzaInstance = amzaInstance;
        this.upenaSSLConfig = upenaSSLConfig;
        this.upenaStore = upenaStore;
        this.healthPluginRegion = healthPluginRegion;
        this.discoveredRoutes = discoveredRoutes;
    }

    @Override
    public String getRootPath() {
        return "/ui/connectivity";
    }

    public static class ConnectivityPluginRegionInput implements PluginInput {

        final String clusterKey;
        final String cluster;
        final String hostKey;
        final String host;
        final String serviceKey;
        final String service;
        final String releaseKey;
        final String release;
        final Set<String> linkType;

        public ConnectivityPluginRegionInput(String clusterKey, String cluster, String hostKey, String host, String serviceKey, String service,
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
            return "Connectivity";
        }

    }

    @Override
    public String render(String user, ConnectivityPluginRegionInput input) {
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
                    String idColor = UpenaHealth.getHEXIdColor((double) i / (double) serviceColor.size(), 1f);
                    serviceNameLegend.add(ImmutableMap.of("name", entrySet.getValue().getValue().name, "color", idColor));
                    i++;
                }
            }

            data.put("serviceLegend", mapper.writeValueAsString(serviceNameLegend));

            connectivityGraph(user, instanceFilter, serviceColor, data);

            for (String ensure : new String[] { "connectivityNodes", "connectivityEdges" }) {
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

    private void connectivityGraph(String user, InstanceFilter filter,
        Map<String, Integer> serviceColor,
        Map<String, Object> data) throws Exception, JsonProcessingException {

        Map<String, Node> nodes = new HashMap<>();
        int id = 0;
        buildClusterRoutes();
        Map<String, Map<String, Map<String, ConnectionHealth>>> routes = discoveredRoutes.from_to_Family_ConnectionHealths;

        for (Map.Entry<String, Map<String, Map<String, ConnectionHealth>>> entrySet : routes.entrySet()) {
            String instanceId = entrySet.getKey();
            Instance instance = upenaStore.instances.get(new InstanceKey(instanceId));
            if (instance != null && !filter.filter(new InstanceKey(instanceId), instance)) {
                continue;
            }

            Service service = null;
            if (instance != null) {
                service = upenaStore.services.get(instance.serviceKey);
            }
            String serviceName = service != null ? service.name : instanceId;
            Node from = nodes.get(serviceName);
            if (from == null) {

                from = new Node(serviceName, id, serviceIdColor(serviceColor, serviceName), "12", 1);
                from.sslEnabled = instance.ports.get("main").sslEnabled;
                from.serviceAuthEnabled = instance.ports.get("main").serviceAuthEnabled;


                id++;
                nodes.put(serviceName, from);

                double serviceHealth = serviceHealth(nannyHealth(instanceId));
                from.maxHealth = Math.max(from.maxHealth, serviceHealth);
                from.minHealth = Math.min(from.minHealth, serviceHealth);
                Instance.Port mainPort = instance.ports.get("main");
                if (mainPort != null) {
                    from.sslCount += mainPort.sslEnabled ? 1 : 0;
                    from.sauthCount += mainPort.serviceAuthEnabled ? 1 : 0;
                }
            } else {
                from.count++;
            }

            Map<String, Map<String, ConnectionHealth>> hostPortFamilyConnectionHealths = entrySet.getValue();
            for (Map.Entry<String, Map<String, ConnectionHealth>> hostPortFamilyConnectionHealth : hostPortFamilyConnectionHealths.entrySet()) {
                Map<String, ConnectionHealth> familyConnectionHealths = hostPortFamilyConnectionHealth.getValue();
                for (Map.Entry<String, ConnectionHealth> familyConnectionHealth : familyConnectionHealths.entrySet()) {
                    ConnectionHealth connectionHealth = familyConnectionHealth.getValue();
                    InstanceDescriptor instanceDescriptor = connectionHealth.connectionDescriptor.getInstanceDescriptor();
                    String toServiceName = instanceDescriptor.serviceName;
                    Node to = nodes.get(toServiceName);
                    if (to == null) {
                        to = new Node(toServiceName, id, serviceIdColor(serviceColor, toServiceName), "12", 1);
                        to.sslEnabled = instanceDescriptor.ports.get("main").sslEnabled;
                        to.serviceAuthEnabled = instanceDescriptor.ports.get("main").serviceAuthEnabled;

                        id++;
                        nodes.put(toServiceName, to);

                        double serviceHealth = serviceHealth(nannyHealth(instanceDescriptor.instanceKey));
                        to.maxHealth = Math.max(to.maxHealth, serviceHealth);
                        to.minHealth = Math.min(to.minHealth, serviceHealth);
                        InstanceDescriptor.InstanceDescriptorPort mainPort = instanceDescriptor.ports.get("main");
                        if (mainPort != null) {
                            from.sslCount += mainPort.sslEnabled ? 1 : 0;
                            from.sauthCount += mainPort.serviceAuthEnabled ? 1 : 0;
                        }
                    }
                }
            }
        }

        Map<String, Edge> edges = new HashMap<>();

        for (Map.Entry<String, Map<String, Map<String, ConnectionHealth>>> entrySet : routes.entrySet()) {
            String instanceId = entrySet.getKey();
            Map<String, Map<String, ConnectionHealth>> to_Family_ConnectionHealths = entrySet.getValue();

            Instance instance = upenaStore.instances.get(new InstanceKey(instanceId));
            if (instance != null && !filter.filter(new InstanceKey(instanceId), instance)) {
                continue;
            }
            Service service = null;
            if (instance != null) {
                service = upenaStore.services.get(instance.serviceKey);
            }
            String serviceName = service != null ? service.name : instanceId;
            Node from = nodes.get(serviceName);

            MinMaxDouble mmd = new MinMaxDouble();

            // TODO fix with async: This is crap because it clobbers
            if (from.focusHtml == null) {
                from.focusHtml = Lists.newArrayList();
            }
            ((List) from.focusHtml).add(renderConnections(mmd, nodes, serviceName, instanceId));

            for (Map.Entry<String, Map<String, ConnectionHealth>> to_Family_ConnectionHealth : to_Family_ConnectionHealths.entrySet()) {

                Map<String, ConnectionHealth> familyConnectionHealths = to_Family_ConnectionHealth.getValue();
                Node to = null;
                MinMaxDouble edgeWeight = new MinMaxDouble();
                double successPerSecond = 0;

                for (Map.Entry<String, ConnectionHealth> familyConnectionHealth : familyConnectionHealths.entrySet()) {
                    ConnectionHealth connectionHealth = familyConnectionHealth.getValue();
                    if (to == null) {
                        InstanceDescriptor instanceDescriptor = connectionHealth.connectionDescriptor.getInstanceDescriptor();
                        String toServiceName = instanceDescriptor.serviceName;
                        to = nodes.get(toServiceName);
                        if (to == null) {
                            to = new Node(toServiceName, id, serviceIdColor(serviceColor, toServiceName), "12", 0);
                            to.sslEnabled = instanceDescriptor.ports.get("main").sslEnabled;
                            to.serviceAuthEnabled = instanceDescriptor.ports.get("main").serviceAuthEnabled;

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


                edge.label = numberFormat.format(successPerSecond) + "/sec ";
            }
        }

        List<Map<String, String>> renderNodes = new ArrayList<>();
        for (Node n : nodes.values()) {
            Map<String, String> node = new HashMap<>();
            node.put("id", "id" + n.id);
            node.put("sslEnabled", String.valueOf(n.sslEnabled));
            node.put("serviceAuthEnabled", String.valueOf(n.serviceAuthEnabled));

            if (n.minHealth == Double.MAX_VALUE) {
                node.put("maxbgcolor", n.bgcolor);
                node.put("minbgcolor", n.bgcolor);
                node.put("healthRadius", "0");
            } else {
                node.put("maxbgcolor", UpenaHealth.getHEXTrafficlightColor(n.maxHealth, 1f));
                node.put("minbgcolor", UpenaHealth.getHEXTrafficlightColor(n.minHealth, 1f));
                node.put("healthRadius", String.valueOf((int) (1d - n.minHealth) * 4));
            }
            node.put("fontSize", n.fontSize);


            node.put("label", n.label + " (" + n.count + ") ");
            node.put("count", String.valueOf(n.count));

            if (n.focusHtml != null && n.focusHtml instanceof List) {
                Map<String, Object> d = new HashMap<>();
                d.put("healths", n.focusHtml);
                n.focusHtml = renderer.render(connectionOverviewTemplate, d);
            }

            node.put("focusHtml", n.focusHtml == null ? "" : n.focusHtml.toString());
            if (n.tooltip != null) {
                node.put("tooltip", n.tooltip);
            }
            node.put("color", n.bgcolor);

            renderNodes.add(node);
        }

        data.put("connectivityNodes", mapper.writeValueAsString(renderNodes));

        List<Map<String, String>> renderEdges = new ArrayList<>();
        for (Edge e : edges.values()) {
            Map<String, String> edge = new HashMap<>();
            edge.put("from", "id" + e.from);
            edge.put("label", e.label);
            edge.put("to", "id" + e.to);
            edge.put("color", UpenaHealth.getHEXIdColor((e.from / (float) id), 1f));
            edge.put("minColor", UpenaHealth.getHEXTrafficlightColor(e.min, 1f));
            edge.put("maxColor", UpenaHealth.getHEXTrafficlightColor(e.max, 1f));
            renderEdges.add(edge);
        }

        data.put("connectivityEdges", mapper.writeValueAsString(renderEdges));

    }

    public String renderInstance(String user, String instanceKey) {
        try {
            Instance renderInstance = upenaStore.instances.get(new InstanceKey(instanceKey));
            String renderService = "unknown";

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
                    if (entrySet.getKey().equals(renderInstance.serviceKey)) {
                        renderService = entrySet.getValue().getValue().name;
                    }
                    serviceColor.put(entrySet.getValue().getValue().name, i);
                    i++;
                }
            }

            Map<String, Node> nodes = new HashMap<>();
            int id = 0;
            buildClusterRoutes();
            Map<String, Map<String, Map<String, ConnectionHealth>>> routes = discoveredRoutes.from_to_Family_ConnectionHealths;

            for (Map.Entry<String, Map<String, Map<String, ConnectionHealth>>> entrySet : routes.entrySet()) {
                String instanceId = entrySet.getKey();
                Instance instance = upenaStore.instances.get(new InstanceKey(instanceId));

                Service service = null;
                if (instance != null) {
                    service = upenaStore.services.get(instance.serviceKey);
                }
                String serviceName = service != null ? service.name : instanceId;
                Node from = nodes.get(serviceName);
                if (from == null) {

                    from = new Node(serviceName, id, serviceIdColor(serviceColor, serviceName), "12", 0);
                    id++;
                    nodes.put(serviceName, from);

                    double serviceHealth = serviceHealth(nannyHealth(instanceId));
                    from.maxHealth = Math.max(from.maxHealth, serviceHealth);
                    from.minHealth = Math.min(from.minHealth, serviceHealth);
                } else {
                    from.count++;
                }

                Map<String, Map<String, ConnectionHealth>> hostPortFamilyConnectionHealths = entrySet.getValue();
                for (Map.Entry<String, Map<String, ConnectionHealth>> hostPortFamilyConnectionHealth : hostPortFamilyConnectionHealths.entrySet()) {
                    Map<String, ConnectionHealth> familyConnectionHealths = hostPortFamilyConnectionHealth.getValue();
                    for (Map.Entry<String, ConnectionHealth> familyConnectionHealth : familyConnectionHealths.entrySet()) {
                        ConnectionHealth connectionHealth = familyConnectionHealth.getValue();
                        String toServiceName = connectionHealth.connectionDescriptor.getInstanceDescriptor().serviceName;
                        Node to = nodes.get(toServiceName);
                        if (to == null) {
                            to = new Node(toServiceName, id, serviceIdColor(serviceColor, toServiceName), "12", 0);
                            id++;
                            nodes.put(toServiceName, to);

                            double serviceHealth = serviceHealth(nannyHealth(connectionHealth.connectionDescriptor.getInstanceDescriptor().instanceKey));
                            to.maxHealth = Math.max(to.maxHealth, serviceHealth);
                            to.minHealth = Math.min(to.minHealth, serviceHealth);
                        }
                    }
                }
            }

            MinMaxDouble mmd = new MinMaxDouble();
            return renderConnectionHealth(mmd, nodes, renderService, instanceKey);
        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
            return "Oops:" + ExceptionUtils.getStackTrace(e);
        }
    }

    private String serviceIdColor(Map<String, Integer> serviceColor, String serviceName) {
        Integer si = serviceColor.get(serviceName);
        if (si == null) {
            si = 0;
        }
        return UpenaHealth.getHEXIdColor(((float) si / (float) serviceColor.size()), 1f);
    }

    private NannyHealth nannyHealth(String instanceId) throws Exception {
        NannyHealth health = null;
        Collection<NodeHealth> nodeHealths = upenaHealth.buildClusterHealth().values();
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

    private Map<String, Object> renderConnections(MinMaxDouble mmd, Map<String, Node> nodes, String from, String instanceId) throws Exception {
        Map<String, Map<String, ConnectionHealth>> connectionHealths = discoveredRoutes.getConnectionHealth(instanceId);

        long success = 0;
        long failure = 0;
        long successPerSecond = 0;
        long failurePerSecond = 0;
        long inflight = 0;

        double latencyMin = 0;
        double latencyMean = 0;
        double latencyMax = 0;

        double latency50th = 0;
        double latency75th = 0;
        double latency90th = 0;
        double latency95th = 0;
        double latency99th = 0;
        double latency999th = 0;

        for (Map.Entry<String, Map<String, ConnectionHealth>> hostPortHealth : connectionHealths.entrySet()) {

            for (Map.Entry<String, ConnectionHealth> familyHealth : hostPortHealth.getValue().entrySet()) {

                ConnectionHealth value = familyHealth.getValue();

                Node fromNode = nodes.get(from);
                if (fromNode == null) {
                    continue;
                }
                success += value.success;
                failure += value.failure;
                successPerSecond += value.successPerSecond;
                failurePerSecond += value.failurePerSecond;
                inflight += (value.attempt - value.success - value.failure);

                latencyMin = Math.min(latencyMin, value.latencyStats.latencyMin);
                latencyMean = Math.max(latencyMean, value.latencyStats.latencyMean);
                latencyMax = Math.max(latencyMax, value.latencyStats.latencyMax);

                latency50th = Math.max(latency50th, value.latencyStats.latency50th);
                latency75th = Math.max(latency999th, value.latencyStats.latency75th);
                latency90th = Math.max(latency999th, value.latencyStats.latency90th);
                latency95th = Math.max(latency999th, value.latencyStats.latency95th);
                latency99th = Math.max(latency999th, value.latencyStats.latency99th);
                latency999th = Math.max(latency999th, value.latencyStats.latency999th);

                mmd.value(latencyMin >= 0d && !Double.isNaN(latencyMin) ? latencyMin : 0);
                mmd.value(latencyMean >= 0d && !Double.isNaN(latencyMean) ? latencyMean : 0);
                mmd.value(latencyMax >= 0d && !Double.isNaN(latencyMax) ? latencyMax : 0);

                mmd.value(latency50th >= 0d && !Double.isNaN(latency50th) ? latency50th : 0);
                mmd.value(latency75th >= 0d && !Double.isNaN(latency75th) ? latency75th : 0);
                mmd.value(latency90th >= 0d && !Double.isNaN(latency90th) ? latency90th : 0);
                mmd.value(latency95th >= 0d && !Double.isNaN(latency95th) ? latency95th : 0);
                mmd.value(latency99th >= 0d && !Double.isNaN(latency99th) ? latency99th : 0);
                mmd.value(latency999th >= 0d && !Double.isNaN(latency999th) ? latency999th : 0);

            }
        }

        String name = "";
        Instance instance = upenaStore.instances.get(new InstanceKey(instanceId));
        if (instance != null) {
            Host host = upenaStore.hosts.get(instance.hostKey);
            name += instance.instanceId + " on " + host.hostName + ":" + instance.ports.get("main").port;
        }

        Map<String, Object> health = new HashMap<>();

        health.put("instanceKey", instanceId);
        health.put("from", from + ":" + name);

        health.put("success", numberFormat.format(success));
        health.put("failure", numberFormat.format(failure));
        health.put("successPerSecond", numberFormat.format(successPerSecond));
        health.put("failurePerSecond", numberFormat.format(failurePerSecond));

        health.put("inflight", numberFormat.format(inflight));

        health.put("min", numberFormat.format(latencyMin));
        health.put("minColor", UpenaHealth.trafficlightColorRGBA(1d - mmd.zeroToOne(latencyMin), 1f));
        health.put("mean", numberFormat.format(latencyMean));
        health.put("meanColor", UpenaHealth.trafficlightColorRGBA(1d - mmd.zeroToOne(latencyMean), 1f));
        health.put("max", numberFormat.format(latencyMax));
        health.put("maxColor", UpenaHealth.trafficlightColorRGBA(1d - mmd.zeroToOne(latencyMax), 1f));

        health.put("latency50th", numberFormat.format(latency50th));
        health.put("latency50thColor", UpenaHealth.trafficlightColorRGBA(1d - mmd.zeroToOne(latency50th), 1f));
        health.put("latency75th", numberFormat.format(latency75th));
        health.put("latency75thColor", UpenaHealth.trafficlightColorRGBA(1d - mmd.zeroToOne(latency75th), 1f));
        health.put("latency90th", numberFormat.format(latency90th));
        health.put("latency90thColor", UpenaHealth.trafficlightColorRGBA(1d - mmd.zeroToOne(latency90th), 1f));
        health.put("latency95th", numberFormat.format(latency95th));
        health.put("latency95thColor", UpenaHealth.trafficlightColorRGBA(1d - mmd.zeroToOne(latency95th), 1f));
        health.put("latency99th", numberFormat.format(latency99th));
        health.put("latency99thColor", UpenaHealth.trafficlightColorRGBA(1d - mmd.zeroToOne(latency99th), 1f));
        health.put("latency999th", numberFormat.format(latency999th));
        health.put("latency999thColor", UpenaHealth.trafficlightColorRGBA(1d - mmd.zeroToOne(latency999th), 1f));

        return health;
    }

    private String renderConnectionHealth(MinMaxDouble mmd, Map<String, Node> nodes, String from, String instanceId) throws Exception {
        List<Map<String, Object>> healths = new ArrayList<>();
        Map<String, Map<String, ConnectionHealth>> connectionHealths = discoveredRoutes.getConnectionHealth(instanceId);

        for (Map.Entry<String, Map<String, ConnectionHealth>> toInstanceHealth : connectionHealths.entrySet()) {

            for (Map.Entry<String, ConnectionHealth> familyHealth : toInstanceHealth.getValue().entrySet()) {

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

        for (Map.Entry<String, Map<String, ConnectionHealth>> hostPortHealth : connectionHealths.entrySet()) {

            for (Map.Entry<String, ConnectionHealth> familyHealth : hostPortHealth.getValue().entrySet()) {

                ConnectionHealth value = familyHealth.getValue();

                Map<String, Object> health = new HashMap<>();
                Node fromNode = nodes.get(from);
                if (fromNode == null) {
                    continue;
                }
                health.put("from", from);
                health.put("fromColor", UpenaHealth.idColorRGB(((float) fromNode.id / (float) nodes.size()), 1f));

                String serviceName = value.connectionDescriptor.getInstanceDescriptor().serviceName;
                health.put("to", serviceName + "(" + value.connectionDescriptor.getInstanceDescriptor().instanceName + ")");

                Node toNode = nodes.get(value.connectionDescriptor.getInstanceDescriptor().serviceName);
                health.put("toColor", UpenaHealth.idColorRGB(((float) toNode.id / (float) nodes.size()), 1f));

                health.put("family", familyHealth.getKey());

                health.put("success", numberFormat.format(value.success));
                health.put("failure", numberFormat.format(value.failure));
                health.put("successPerSecond", numberFormat.format(value.successPerSecond));
                health.put("failurePerSecond", numberFormat.format(value.failurePerSecond));

                health.put("inflight", numberFormat.format(value.attempt - value.success - value.failure));

                health.put("min", numberFormat.format(value.latencyStats.latencyMin));
                health.put("minColor", UpenaHealth.trafficlightColorRGBA(1d - mmd.zeroToOne(value.latencyStats.latencyMin), 1f));
                health.put("mean", numberFormat.format(value.latencyStats.latencyMean));
                health.put("meanColor", UpenaHealth.trafficlightColorRGBA(1d - mmd.zeroToOne(value.latencyStats.latencyMean), 1f));
                health.put("max", numberFormat.format(value.latencyStats.latencyMax));
                health.put("maxColor", UpenaHealth.trafficlightColorRGBA(1d - mmd.zeroToOne(value.latencyStats.latencyMax), 1f));

                health.put("latency50th", numberFormat.format(value.latencyStats.latency50th));
                health.put("latency50thColor", UpenaHealth.trafficlightColorRGBA(1d - mmd.zeroToOne(value.latencyStats.latency50th), 1f));
                health.put("latency75th", numberFormat.format(value.latencyStats.latency75th));
                health.put("latency75thColor", UpenaHealth.trafficlightColorRGBA(1d - mmd.zeroToOne(value.latencyStats.latency75th), 1f));
                health.put("latency90th", numberFormat.format(value.latencyStats.latency90th));
                health.put("latency90thColor", UpenaHealth.trafficlightColorRGBA(1d - mmd.zeroToOne(value.latencyStats.latency90th), 1f));
                health.put("latency95th", numberFormat.format(value.latencyStats.latency95th));
                health.put("latency95thColor", UpenaHealth.trafficlightColorRGBA(1d - mmd.zeroToOne(value.latencyStats.latency95th), 1f));
                health.put("latency99th", numberFormat.format(value.latencyStats.latency99th));
                health.put("latency99thColor", UpenaHealth.trafficlightColorRGBA(1d - mmd.zeroToOne(value.latencyStats.latency99th), 1f));
                health.put("latency999th", numberFormat.format(value.latencyStats.latency999th));
                health.put("latency999thColor", UpenaHealth.trafficlightColorRGBA(1d - mmd.zeroToOne(value.latencyStats.latency999th), 1f));

                health.put("host", value.connectionDescriptor.getHostPort().getHost());
                health.put("port", value.connectionDescriptor.getHostPort().getPort());
                health.put("sslEnabled", value.connectionDescriptor.getSslEnabled());
                health.put("serviceAuthEnabled", value.connectionDescriptor.getServiceAuthEnabled());
                health.put("instanceKey", value.connectionDescriptor.getInstanceDescriptor().instanceKey);

                healths.add(health);
            }
        }

        Collections.sort(healths, (Map<String, Object> o1, Map<String, Object> o2) -> {

            return ((String) o1.get("family")).compareTo((String) o2.get("family"));
        });

        List<String> description = Lists.newArrayList();
        Instance instance = upenaStore.instances.get(new InstanceKey(instanceId));
        if (instance != null) {
            Cluster cluster = upenaStore.clusters.get(instance.clusterKey);
            Host host = upenaStore.hosts.get(instance.hostKey);
            Service service = upenaStore.services.get(instance.serviceKey);
            ReleaseGroup release = upenaStore.releaseGroups.get(instance.releaseGroupKey);
            description.add("Cluster:" + cluster.name);
            description.add("Datacenter:" + host.datacenterName + " Rack:" + host.rackName + " Host:" + host.hostName + " Name:" + host.name);
            description.add("Service:" + service.name + " Release:" + release.version);
            description.add("Ports:" + instance.ports.toString());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("description", description);
        data.put("healths", healths);

        return renderer.render(connectionHealthTemplate, data);
    }

    private Edge addEdge(Map<String, Edge> edges, Node from, Node to) {
        Edge edge = edges.get(from.id + "->" + to.id);
        if (edge == null) {
            edge = new Edge(from.id, to.id, "");
            edges.put(from.id + "->" + to.id, edge);
            LOG.info("edge:" + edge);
        }
        return edge;
    }

    public static class Node {

        String label;
        String icon;
        int id;
        String bgcolor;
        String fontSize;
        Object focusHtml;
        String tooltip;
        int count = 1;
        int sslCount = 0;
        int sauthCount = 0;
        double maxHealth = -Double.MAX_VALUE;
        double minHealth = Double.MAX_VALUE;
        boolean sslEnabled;
        boolean serviceAuthEnabled;

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

        for (final RingHost ringHost : amzaInstance.getRing("MASTER")) {
            if (currentlyExecuting.putIfAbsent(ringHost, true) == null) {
                executorService.submit(() -> {
                    long start = System.currentTimeMillis();
                    String nodeKey = ringHost.getHost() + ":" + ringHost.getPort();

                    try {
                        Long last = nodeRecency.get(nodeKey);
                        long sinceTimestampMillis = last == null ? 0 : last;
                        HttpRequestHelper requestHelper = HttpRequestHelperUtils.buildRequestHelper(upenaSSLConfig.sslEnable,
                            upenaSSLConfig.allowSelfSignedCerts, upenaSSLConfig.signer, ringHost.getHost(), ringHost.getPort());
                        RouteHealths routeHealths = requestHelper.executeGetRequest("/upena/routes/health/" + sinceTimestampMillis, RouteHealths.class, null);
                        for (InstanceConnectionHealth routeHealth : routeHealths.getRouteHealths()) {
                            discoveredRoutes.connectionHealth(routeHealth);
                        }
                    } catch (Exception x) {
                        LOG.warn("Failed getting route health for instances " + ringHost, x);
                    }

                    try {
                        HttpRequestHelper requestHelper = HttpRequestHelperUtils.buildRequestHelper(upenaSSLConfig.sslEnable,
                            upenaSSLConfig.allowSelfSignedCerts, upenaSSLConfig.signer, ringHost.getHost(), ringHost.getPort());
                        Routes routes = requestHelper.executeGetRequest("/upena/routes/instances", Routes.class, null);
                        nodeRoutes.put(ringHost, routes);
                    } catch (Exception x) {
                        Routes routes = new Routes(Collections.emptyList());
                        nodeRoutes.put(ringHost, routes);
                        LOG.warn("Failed getting routes for instances " + ringHost, x);
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
