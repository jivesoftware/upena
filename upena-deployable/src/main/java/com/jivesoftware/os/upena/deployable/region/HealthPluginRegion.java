package com.jivesoftware.os.upena.deployable.region;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jivesoftware.os.amza.shared.AmzaInstance;
import com.jivesoftware.os.amza.shared.RingHost;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.endpoints.base.HasUI;
import com.jivesoftware.os.routing.bird.endpoints.base.HasUI.UI;
import com.jivesoftware.os.routing.bird.http.client.HttpClient;
import com.jivesoftware.os.routing.bird.http.client.HttpClientConfig;
import com.jivesoftware.os.routing.bird.http.client.HttpClientConfiguration;
import com.jivesoftware.os.routing.bird.http.client.HttpClientFactory;
import com.jivesoftware.os.routing.bird.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptor;
import com.jivesoftware.os.upena.config.UpenaConfigStore;
import com.jivesoftware.os.upena.deployable.UpenaEndpoints;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterFilter;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostFilter;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupFilter;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceFilter;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
// soy.page.healthPluginRegion
public class HealthPluginRegion implements PageRegion<HealthPluginRegion.HealthPluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final long startupTime;
    private final RingHost ringHost;
    private final String template;
    private final String instanceTemplate;
    private final SoyRenderer renderer;
    private final AmzaInstance amzaInstance;
    private final UpenaStore upenaStore;
    private final UpenaConfigStore configStore;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private final Map<InstanceDescriptor, SparseCircularHitsBucketBuffer> instanceHealthHistory = new ConcurrentHashMap<>();

    public HealthPluginRegion(long startupTime,
        RingHost ringHost,
        String template,
        String instanceTemplate,
        SoyRenderer renderer,
        AmzaInstance amzaInstance,
        UpenaStore upenaStore,
        UpenaConfigStore configStore) {

        this.startupTime = startupTime;
        this.ringHost = ringHost;
        this.template = template;
        this.instanceTemplate = instanceTemplate;
        this.renderer = renderer;
        this.amzaInstance = amzaInstance;
        this.upenaStore = upenaStore;
        this.configStore = configStore;
    }

    @Override
    public String getRootPath() {
        return "/ui/health";
    }

    public Map<String, Object> poll(HealthPluginRegionInput healthPluginRegionInput) throws Exception {

        Map<ServiceKey, String> serviceColor = ServiceColorUtil.serviceKeysColor(upenaStore);

        List<String> labels = new ArrayList<>();
        List<Map<String, Object>> valueDatasets = new ArrayList<>();

        int labelCount = 0;
        for (Map.Entry<InstanceDescriptor, SparseCircularHitsBucketBuffer> waveforms : instanceHealthHistory.entrySet()) {
            InstanceDescriptor id = waveforms.getKey();
            List<String> values = new ArrayList<>();
            SparseCircularHitsBucketBuffer buffer = waveforms.getValue();
            double[] rawSignal = buffer.rawSignal();
            labelCount = Math.max(labelCount, rawSignal.length);
            double lastValue = 0d;
            for (double d : rawSignal) {
                values.add(String.valueOf(d));
                lastValue = d;
            }
            Map<String, Object> w = waveform(id.serviceName + "-" + id.instanceName,
                trafficlightColorRGB(lastValue, 1f),
                serviceColor.getOrDefault(new ServiceKey(id.serviceKey), "127,127,127"),
                values);
            valueDatasets.add(w);
        }

        for (int i = 0; i < labelCount; i++) {
            labels.add("");
        }

        Map<String, Object> map = new HashMap<>();
        map.put("id", "health-waveform");
        map.put("graphType", "Line");
        map.put("waveforms", ImmutableMap.of("labels", labels, "datasets", valueDatasets));
        return map;

    }

    public Map<String, Object> waveform(String label,
        String color,
        String pointColor,
        List<String> values) {
        Map<String, Object> waveform = new HashMap<>();
        waveform.put("label", label);
        waveform.put("fillColor", "rgba(" + color + ",0.4)");
        waveform.put("strokeColor", "rgba(" + color + ",0.4)");
        waveform.put("pointColor", "rgba(" + pointColor + ",0.4)");
        waveform.put("pointStrokeColor", "rgba(" + pointColor + ",0.4)");
        waveform.put("pointHighlightFill", "rgba(" + color + ",0.4)");
        waveform.put("pointHighlightStroke", "rgba(" + color + ",0.4)");
        waveform.put("data", values);
        return waveform;
    }

    public static class HealthPluginRegionInput implements PluginInput {

        final String datacenter;
        final String rack;
        final String cluster;
        final String host;
        final String service;

        public HealthPluginRegionInput(String datacenter, String rack, String cluster, String host, String service) {
            this.datacenter = datacenter;
            this.rack = rack;
            this.cluster = cluster;
            this.host = host;
            this.service = service;
        }

        @Override
        public String name() {
            return "Health";
        }
    }

    public String renderLive(String user, HealthPluginRegionInput input) {

        String live = "[]";
        try {

            List<Map<String, Object>> healths = new ArrayList<>();

            Map<String, Double> minHostHealth = new HashMap<>();
            Map<String, Double> minServiceHealth = new HashMap<>();
            for (UpenaEndpoints.NodeHealth nodeHealth : buildClusterHealth().values()) {

                for (UpenaEndpoints.NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                    if (nannyHealth.serviceHealth != null) {
                        if (nannyHealth.serviceHealth != null) {

                            double h = Math.max(0d, nannyHealth.serviceHealth.health);
                            if (nannyHealth.instanceDescriptor.enabled) {

                                Instance instance = upenaStore.instances.get(new InstanceKey(nannyHealth.instanceDescriptor.instanceKey));

                                String label = String.valueOf((int) (h * 100));
                                String age = nannyHealth.uptime;
                                String color = trafficlightColorRGB(h, 1f);
                                long now = System.currentTimeMillis();
                                if (instance != null
                                    && instance.restartTimestampGMTMillis > 0
                                    && System.currentTimeMillis() < instance.restartTimestampGMTMillis) {
                                    age = UpenaEndpoints.shortHumanReadableUptime(instance.restartTimestampGMTMillis - now);
                                    color = "255,105,180";
                                    label = "Restarting";
                                }

                                if (nannyHealth.status != null && nannyHealth.status.length() > 0) {
                                    label = nannyHealth.status;
                                }

                                minHostHealth.compute(nannyHealth.instanceDescriptor.clusterName + ":" + nodeHealth.host + ":" + nodeHealth.port,
                                    (String k, Double ev) -> {
                                        return ev == null ? nannyHealth.serviceHealth.health : Math.min(ev, nannyHealth.serviceHealth.health);
                                    });

                                minServiceHealth.compute(nannyHealth.instanceDescriptor.clusterName + ":" + nannyHealth.instanceDescriptor.serviceName,
                                    (String k, Double ev) -> {
                                        return ev == null ? nannyHealth.serviceHealth.health : Math.min(ev, nannyHealth.serviceHealth.health);
                                    });

                                ImmutableMap.Builder<String, Object> map = ImmutableMap.<String, Object>builder()
                                    .put("id", nannyHealth.instanceDescriptor.instanceKey)
                                    .put("color", color)
                                    .put("text", label)
                                    .put("age", age);

                                if (nannyHealth.unexpectedRestart > -1) {
                                    map.put("unexpectedRestart", UpenaEndpoints.humanReadableUptime(now - nannyHealth.unexpectedRestart));
                                }

                                if (!nannyHealth.configIsStale.isEmpty()) {
                                    map.put("configIsStale", nannyHealth.configIsStale);
                                }

                                if (!nannyHealth.healthConfigIsStale.isEmpty()) {
                                    map.put("healthConfigIsStale", nannyHealth.healthConfigIsStale);
                                }

                                healths.add(map.build());
                            } else {
                                healths.add(ImmutableMap.<String, Object>builder()
                                    .put("id", nannyHealth.instanceDescriptor.instanceKey)
                                    .put("color", "128,128,128")
                                    .put("text", "")
                                    .put("age", "-")
                                    .build());
                            }
                        }
                    }
                }
            }

            for (Map.Entry<String, Double> m : minHostHealth.entrySet()) {

                String[] parts = m.getKey().split(":");
                Long recency = nodeRecency.get(parts[1] + ":" + parts[2]);
                String age = recency != null
                    ? UpenaEndpoints.shortHumanReadableUptime(System.currentTimeMillis() - recency)
                    : ">" + UpenaEndpoints.shortHumanReadableUptime(System.currentTimeMillis() - startupTime);

                healths.add(ImmutableMap.<String, Object>builder()
                    .put("id", m.getKey())
                    .put("color", trafficlightColorRGB(Math.max(m.getValue(), 0d), 1f))
                    .put("text", m.getKey())
                    .put("age", age)
                    .build());
            }

            live = MAPPER.writeValueAsString(healths);
        } catch (Exception x) {
            LOG.warn("failed to generate live results", x);
        }

        return live;

    }

    static class GridHost implements Comparable<GridHost> {

        private final String datacenter;
        private final String rack;
        private final String clusterName;
        private final String hostName;
        private final int port;

        public GridHost(String datacenter, String rack, String clusterName, String hostName, int port) {
            this.datacenter = datacenter;
            this.rack = rack;
            this.clusterName = clusterName;
            this.hostName = hostName;
            this.port = port;
        }

        @Override
        public String toString() {
            return clusterName + ":" + hostName + ":" + port;
        }

        @Override
        public int compareTo(GridHost o) {
            int c = datacenter.compareTo(o.datacenter);
            if (c != 0) {
                return c;
            }
            c = rack.compareTo(o.rack);
            if (c != 0) {
                return c;
            }
            c = clusterName.compareTo(o.clusterName);
            if (c != 0) {
                return c;
            }
            c = hostName.compareTo(o.hostName);
            if (c != 0) {
                return c;
            }
            return Integer.compare(port, o.port);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + Objects.hashCode(this.clusterName);
            hash = 79 * hash + Objects.hashCode(this.hostName);
            hash = 79 * hash + this.port;
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
            final GridHost other = (GridHost) obj;
            if (this.port != other.port) {
                return false;
            }
            if (!Objects.equals(this.clusterName, other.clusterName)) {
                return false;
            }
            if (!Objects.equals(this.hostName, other.hostName)) {
                return false;
            }
            return true;
        }

    }

    @Override
    public String render(String user, HealthPluginRegionInput input) {
        Map<String, Object> data = Maps.newHashMap();

        try {

            //Map<ServiceKey, String> serviceColor = ServiceColorUtil.serviceKeysColor(upenaStore);

            Map<String, String> filter = new HashMap<>();
            filter.put("datacenter", input.datacenter);
            filter.put("rack", input.rack);
            filter.put("cluster", input.cluster);
            filter.put("host", input.host);
            filter.put("service", input.service);
            data.put("filter", filter);

            ConcurrentMap<RingHost, UpenaEndpoints.NodeHealth> nodeHealths = buildClusterHealth();

            Map<String, Double> minClusterHealth = new HashMap<>();
            for (UpenaEndpoints.NodeHealth nodeHealth : nodeHealths.values()) {
                for (UpenaEndpoints.NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                    if (nannyHealth.serviceHealth != null) {
                        Double got = minClusterHealth.get(nannyHealth.instanceDescriptor.clusterKey);
                        if (got == null || got > nannyHealth.serviceHealth.health) {
                            minClusterHealth.put(nannyHealth.instanceDescriptor.clusterKey, nannyHealth.serviceHealth.health);
                        }
                    }
                }
            }

            ConcurrentSkipListSet<GridHost> gridHosts = new ConcurrentSkipListSet<>();
            ConcurrentSkipListSet<GridService> services = new ConcurrentSkipListSet<>((GridService o1, GridService o2) -> {
                int c = o1.serviceName.compareTo(o2.serviceName);
                if (c != 0) {
                    return c;
                }
                return o1.serviceKey.compareTo(o2.serviceKey);
            });

            // Services
             Map<ServiceKey, TimestampedValue<Service>> foundServices = upenaStore.services.find(new ServiceFilter(null, null, 0, 10000));
            List<Map<String, String>> serviceResults = Lists.newArrayList();
            for (Map.Entry<ServiceKey, TimestampedValue<Service>> entry : foundServices.entrySet()) {
                serviceResults.add(ImmutableMap.of("key", entry.getKey().getKey(), "name", entry.getValue().getValue().name));
                services.add(new GridService(entry.getKey().getKey(), entry.getValue().getValue().name));
            }
            sort(serviceResults);
            data.put("services", serviceResults);


            Map<String, Map<String, String>> instanceHealth = new HashMap<>();

            for (UpenaEndpoints.NodeHealth nodeHealth : nodeHealths.values()) {
                if (nodeHealth.nannyHealths.isEmpty()) {
                    gridHosts.add(new GridHost("UNREACHABLE", "UNREACHABLE", "UNREACHABLE", nodeHealth.host, nodeHealth.port));
                }
                for (UpenaEndpoints.NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                    boolean dshow = input.datacenter.isEmpty() ? false : nannyHealth.instanceDescriptor.datacenter.contains(input.datacenter);
                    boolean rshow = input.rack.isEmpty() ? false : nannyHealth.instanceDescriptor.rack.contains(input.rack);
                    boolean cshow = input.cluster.isEmpty() ? false : nannyHealth.instanceDescriptor.clusterName.contains(input.cluster);
                    boolean hshow = input.host.isEmpty() ? false : nodeHealth.host.contains(input.host);
                    boolean sshow = input.service.isEmpty() ? false : nannyHealth.instanceDescriptor.serviceName.contains(input.service);

                    if ((!input.datacenter.isEmpty() == dshow)
                        && (!input.rack.isEmpty() == rshow)
                        && (!input.cluster.isEmpty() == cshow)
                        && (!input.host.isEmpty() == hshow)
                        && (!input.service.isEmpty() == sshow)) {

                        GridHost gridHost = new GridHost(nannyHealth.instanceDescriptor.datacenter, nannyHealth.instanceDescriptor.rack,
                            nannyHealth.instanceDescriptor.clusterName, nodeHealth.host, nodeHealth.port);
                        if (!gridHosts.contains(gridHost)) {
                            gridHosts.add(gridHost);
                        }
                        GridService service = new GridService(nannyHealth.instanceDescriptor.serviceKey, nannyHealth.instanceDescriptor.serviceName);
                        if (!services.contains(service)) {
                            services.add(service);
                        }

                        Map<String, String> h = new HashMap<>();
                        Double ch = minClusterHealth.get(nannyHealth.instanceDescriptor.clusterKey);
                        h.put("cluster", "<div title=\"" + nannyHealth.instanceDescriptor.clusterName
                            + "\" style=\"background-color:#" + getHEXTrafficlightColor(ch, 1f) + "\">"
                            + d2f(ch) + "</div>");

                        Double nh = nodeHealth.health;
                        h.put("host", "<div title=\"" + nodeHealth.host + ":" + nodeHealth.port
                            + "\" style=\"background-color:#" + getHEXTrafficlightColor(nh, 1f) + "\">"
                            + d2f(nh) + "</div>");

                        Double sh = 0d;
                        if (nannyHealth.serviceHealth != null) {
                            sh = nannyHealth.serviceHealth.health;
                        }
                        h.put("service", "<div title=\"" + nannyHealth.instanceDescriptor.serviceName
                            + "\" style=\"background-color:#" + getHEXTrafficlightColor(sh, 1f) + "\">"
                            + d2f(sh) + "</div>");

                        h.put("key", nannyHealth.instanceDescriptor.instanceKey);
                        h.put("clusterName", nannyHealth.instanceDescriptor.clusterName);
                        h.put("serviceName", nannyHealth.instanceDescriptor.serviceName + " " + nannyHealth.instanceDescriptor.instanceName);
                        h.put("port", String.valueOf(nannyHealth.instanceDescriptor.ports.get("main").port));
                        h.put("hostName", nodeHealth.host);

                        instanceHealth.put(nannyHealth.instanceDescriptor.instanceKey, h);
                    }
                }
            }
            
            // Hosts
            Map<HostKey, TimestampedValue<Host>> foundHosts = upenaStore.hosts.find(new HostFilter(null, null, null, null, null, 0, 10000));
            List<Map<String, String>> hostResults = Lists.newArrayList();
            for (Map.Entry<HostKey, TimestampedValue<Host>> entry : foundHosts.entrySet()) {
                String name = entry.getValue().getValue().hostName + "/" + entry.getValue().getValue().name;
                if (entry.getValue().getValue().name.equals(entry.getValue().getValue().hostName)) {
                    name = entry.getValue().getValue().hostName;
                }
                hostResults.add(ImmutableMap.of("key", entry.getKey().getKey(), "name", name));
            }
            sort(hostResults);
            data.put("hosts", hostResults);


            Map<GridServiceKey, GridService> serviceIndexs = new HashMap<>();
            int serviceIndex = 0;
            for (GridService service : services) {
                service.index = serviceIndex;
                serviceIndexs.put(new GridServiceKey(service.serviceKey, service.serviceName), service);
                serviceIndex++;
            }
            Map<GridHost, Integer> hostIndexs = new HashMap<>();
            int hostIndex = 0;
            int uid = 0;
            List<List<Map<String, Object>>> hostRows = new ArrayList<>();
            String lastDatacenter = null;
            String lastRack = null;
            for (GridHost gridHost : gridHosts) {

                List<Map<String, Object>> hostRow = new ArrayList<>();
                String currentDatacenter = com.google.common.base.Objects.firstNonNull(gridHost.datacenter, "");
                String currentRack = com.google.common.base.Objects.firstNonNull(gridHost.rack, "");
                if (lastDatacenter == null || !lastDatacenter.equals(currentDatacenter)
                    || lastRack == null || !lastRack.equals(currentRack)) {

                    lastDatacenter = currentDatacenter;
                    lastRack = currentRack;

                    Map<String, Object> healthCell = new HashMap<>();
                    healthCell.put("datacenter", lastDatacenter);
                    healthCell.put("rack", lastRack);
                    hostRow.add(healthCell);
                    for (int s = 0; s < services.size(); s++) {
                        HashMap<String, Object> cell = new HashMap<>();
                        hostRow.add(cell);
                    }
                    hostRows.add(hostRow);
                    hostRow = new ArrayList<>();
                    hostIndex++;
                }

                hostIndexs.put(gridHost, hostIndex);
                hostIndex++;

                Map<String, Object> healthCell = new HashMap<>();
                healthCell.put("uid", "uid-" + uid);
                uid++;
                healthCell.put("color", "#eee");
                healthCell.put("health", null);
                hostRow.add(healthCell);
                for (int s = 0; s < services.size(); s++) {
                    healthCell = new HashMap<>();
                    healthCell.put("uid", "uid-" + uid);
                    uid++;
                    healthCell.put("color", "#eee");
                    healthCell.put("health", null);
                    hostRow.add(healthCell);
                }
                hostRows.add(hostRow);
            }

            for (UpenaEndpoints.NodeHealth nodeHealth : nodeHealths.values()) {

                Host upenaHost = upenaStore.hosts.get(new HostKey(nodeHealth.hostKey));
                Map<String, String> hostInfo = null;
                if (upenaHost != null) {
                    hostInfo = new HashMap<>();
                    hostInfo.put("publicHost", com.google.common.base.Objects.firstNonNull(upenaHost.name, "unknownPublicHost"));
                    hostInfo.put("datacenter", com.google.common.base.Objects.firstNonNull(upenaHost.datacenterName, "unknownDatacenter"));
                    hostInfo.put("rack", com.google.common.base.Objects.firstNonNull(upenaHost.rackName, "unknownRack"));
                } else {
                    LOG.warn("Failed to locate host for " + nodeHealth.hostKey + " " + nodeHealth.host);
                }

                if (nodeHealth.nannyHealths.isEmpty()) {
                    GridHost host = new GridHost("UNREACHABLE", "UNREACHABLE", "UNREACHABLE", nodeHealth.host, nodeHealth.port);
                    Integer hi = hostIndexs.get(host);
                    if (hi != null) {

                        Long recency = nodeRecency.get(nodeHealth.host + ":" + nodeHealth.port);
                        String age = recency != null
                            ? UpenaEndpoints.shortHumanReadableUptime(System.currentTimeMillis() - recency)
                            : ">" + UpenaEndpoints.shortHumanReadableUptime(System.currentTimeMillis() - startupTime);

                        //float hh = (float) Math.max(0, nodeHealth.health);
                        hostRows.get(hi).get(0).put("color", "#d7d7d7"); // + getHEXTrafficlightColor(hh, 1f));
                        hostRows.get(hi).get(0).put("host", nodeHealth.host); // TODO change to hostKey
                        hostRows.get(hi).get(0).put("hostKey", nodeHealth.host); // TODO change to hostKey
                        hostRows.get(hi).get(0).put("health", host.toString().replace(":", " "));
                        hostRows.get(hi).get(0).put("age", age);
                        hostRows.get(hi).get(0).put("uid", "uid-" + uid);
                        hostRows.get(hi).get(0).put("instanceKey", "");
                        hostRows.get(hi).get(0).put("hostInfo", hostInfo);
                        uid++;
                    }
                }
                for (UpenaEndpoints.NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                    InstanceDescriptor id = nannyHealth.instanceDescriptor;
                    boolean dshow = input.datacenter.isEmpty() ? false : id.datacenter.contains(input.datacenter);
                    boolean rshow = input.rack.isEmpty() ? false : id.rack.contains(input.rack);
                    boolean cshow = input.cluster.isEmpty() ? false : id.clusterName.contains(input.cluster);
                    boolean hshow = input.host.isEmpty() ? false : nodeHealth.host.contains(input.host);
                    boolean sshow = input.service.isEmpty() ? false : id.serviceName.contains(input.service);

                    if ((!input.cluster.isEmpty() == dshow)
                        && (!input.cluster.isEmpty() == rshow)
                        && (!input.cluster.isEmpty() == cshow)
                        && (!input.host.isEmpty() == hshow)
                        && (!input.service.isEmpty() == sshow)) {

                        GridHost host = new GridHost(id.datacenter, id.rack, id.clusterName, nodeHealth.host, nodeHealth.port);
                        Integer hi = hostIndexs.get(host);

                        GridServiceKey serviceIndexKey = new GridServiceKey(id.serviceKey, id.serviceName);
                        GridService service = serviceIndexs.get(serviceIndexKey);
                        if (hi != null && service != null) {

                            ServiceStats serviceStats = service.clusterToServiceStats.computeIfAbsent(id.clusterName,
                                (String clusterName) -> {
                                    return new ServiceStats();
                                });

                            serviceStats.numberInstance.incrementAndGet();
                            if (upenaHost != null) {
                                serviceStats.datacenters.computeIfAbsent(com.google.common.base.Objects.firstNonNull(upenaHost.datacenterName,
                                    "unknownDatacenter"),
                                    (key) -> new AtomicInteger()).incrementAndGet();
                                serviceStats.racks.computeIfAbsent(com.google.common.base.Objects.firstNonNull(upenaHost.rackName, "unknownRack"),
                                    (key) -> new AtomicInteger()).incrementAndGet();
                            }

                            int si = service.index;
                            Long recency = nodeRecency.get(nodeHealth.host + ":" + nodeHealth.port);
                            String age = recency != null ? 
                                UpenaEndpoints.shortHumanReadableUptime(System.currentTimeMillis() - recency)
                                : ">" + UpenaEndpoints.shortHumanReadableUptime(System.currentTimeMillis() - startupTime);

                            //float hh = (float) Math.max(0, nodeHealth.health);
                            hostRows.get(hi).get(0).put("color", "#d7d7d7");// + getHEXTrafficlightColor(hh, 1f));
                            hostRows.get(hi).get(0).put("host", nodeHealth.host); 
                            hostRows.get(hi).get(0).put("hostKey", nodeHealth.hostKey);
                            hostRows.get(hi).get(0).put("health", host.toString().replace(":", " "));
                            hostRows.get(hi).get(0).put("age", age);
                            hostRows.get(hi).get(0).put("uid", "uid-" + uid);
                            hostRows.get(hi).get(0).put("instanceKey", "");
                            hostRows.get(hi).get(0).put("hostInfo", hostInfo);
                            uid++;

                            double h = 0d;
                            if (nannyHealth.serviceHealth != null) {
                                h = nannyHealth.serviceHealth.health;
                            }
                            float sh = (float) Math.max(0, h);
                            Map<String, Object> cell = hostRows.get(hi).get(si + 1);
                            cell.put("uid", "uid-" + uid);
                            uid++;
                            cell.put("instanceKey", id.instanceKey);
                            cell.put("clusterKey", id.clusterKey);
                            cell.put("cluster", id.clusterName);
                            cell.put("serviceKey", id.serviceKey);
                            cell.put("service", id.serviceName);
                            cell.put("releaseKey", id.releaseGroupKey);
                            cell.put("release", id.releaseGroupName);
                            cell.put("version", id.versionName);
                            cell.put("instance", String.valueOf(id.instanceName));
                            if (id.enabled) {
                                cell.put("color", "#" + getHEXTrafficlightColor(sh, 1f));
                                cell.put("health", d2f(sh));
                                cell.put("age", nannyHealth.uptime);

                                if (nannyHealth.unexpectedRestart > -1) {
                                    cell.put("unexpectedRestart",
                                        UpenaEndpoints.humanReadableUptime(System.currentTimeMillis() - nannyHealth.unexpectedRestart));
                                }
                                if (!nannyHealth.configIsStale.isEmpty()) {
                                    cell.put("configIsStale", nannyHealth.configIsStale);
                                }

                                if (!nannyHealth.healthConfigIsStale.isEmpty()) {
                                    cell.put("healthConfigIsStale", nannyHealth.healthConfigIsStale);
                                }

                            } else {
                                cell.put("color", "#404040");
                                cell.put("health", "-");
                                cell.put("age", "-");
                            }
                            cell.put("link", "http://" + nodeHealth.host + ":" + id.ports.get("manage").port + "/manage/ui");

                            List<Map<String, String>> got = (List<Map<String, String>>) cell.get("instances");
                            if (got == null) {
                                got = new ArrayList<>();
                                cell.put("instances", got);
                            }
                            got.add(instanceHealth.get(id.instanceKey));
                        }

                    }
                }
            }

            List<Map<String, Object>> serviceData = new ArrayList<>();
            for (GridService service : services) {
                Map<String, Object> serviceCell = new HashMap<>();
                serviceCell.put("service", service.serviceName);

                addWarning(service);
                if (!service.warnings.isEmpty()) {
                    serviceCell.put("warnings", service.warnings);
                }
                serviceCell.put("serviceKey", service.serviceKey);
                serviceCell.put("serviceColor", "215,215,215"); //serviceColor.getOrDefault(new ServiceKey(service.serviceKey), "127,127,127"));
                serviceData.add(serviceCell);
            }
            data.put("gridServices", serviceData);
            data.put("gridHost", hostRows);

           
            // Clusters
            Map<ClusterKey, TimestampedValue<Cluster>> foundClusters = upenaStore.clusters.find(new ClusterFilter(null, null, 0, 10000));
            List<Map<String, String>> clusterResults = Lists.newArrayList();
            for (Map.Entry<ClusterKey, TimestampedValue<Cluster>> entry : foundClusters.entrySet()) {
                clusterResults.add(ImmutableMap.of("key", entry.getKey().getKey(), "name", entry.getValue().getValue().name));
            }
            sort(clusterResults);
            data.put("clusters", clusterResults);

            
            // Releases
            Map<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> foundReleases = upenaStore.releaseGroups.find(new ReleaseGroupFilter(null, null, null, null, null, 0, 10000));
            List<Map<String, String>> releaseResults = Lists.newArrayList();
            for (Map.Entry<ReleaseGroupKey, TimestampedValue<ReleaseGroup>> entry : foundReleases.entrySet()) {
                releaseResults.add(ImmutableMap.of("key", entry.getKey().getKey(), "name", entry.getValue().getValue().name));
            }
            sort(releaseResults);
            data.put("releases", releaseResults);

        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    private void sort(List<Map<String, String>> clusterResults) {
        Collections.sort(clusterResults, (Map<String, String> o1, Map<String, String> o2) -> {
            int c = o1.get("name").compareTo(o2.get("name"));
            if (c != 0) {
                return c;
            }
            return o1.get("key").compareTo(o2.get("key"));
        });
    }

    public static void addWarning(GridService service) {
        for (String clusterName : service.clusterToServiceStats.keySet()) {
            ServiceStats stats = service.clusterToServiceStats.get(clusterName);
            if (stats.numberInstance.get() < 2) {
                service.warnings.add("WARNING cluster:" + clusterName + " service:" + service.serviceName + " is not HA.");
            }
            if (stats.racks.size() == 1 && stats.numberInstance.get() > 1) {
                service.warnings.add("WARNING cluster:" + clusterName + " service:" + service.serviceName + " is vulnerable to switch/rack failure.");
            }
            if (stats.racks.size() > 1) {
                long score = 1;
                for (AtomicInteger value : stats.racks.values()) {
                    score *= value.get();
                }

                int idealPerRack = stats.numberInstance.get() / stats.racks.size();
                double ratio = score / Math.pow(idealPerRack, stats.racks.size());

                if (ratio < 0.8) {
                    service.warnings.add("WARNING cluster:" + clusterName + "service:" + service.serviceName + " has a poor rack distribution.");
                }
            }
        }
    }

    static class GridServiceKey implements Comparable<GridServiceKey> {

        String serviceKey;
        String serviceName;

        public GridServiceKey(String serviceKey, String serviceName) {
            this.serviceKey = serviceKey;
            this.serviceName = serviceName;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 47 * hash + Objects.hashCode(this.serviceKey);
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
            final GridServiceKey other = (GridServiceKey) obj;
            if (!Objects.equals(this.serviceKey, other.serviceKey)) {
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(GridServiceKey o) {
            return (serviceName + ":" + serviceKey).compareTo(o.serviceName + ":" + o.serviceKey);
        }

    }

    static class ServiceStats {

        AtomicInteger numberInstance = new AtomicInteger(0);
        Map<String, AtomicInteger> racks = new HashMap<>();
        Map<String, AtomicInteger> datacenters = new HashMap<>();
    }

    static class GridService implements Comparable<GridService> {

        String serviceKey;
        String serviceName;
        List<String> warnings = new ArrayList<>();

        Map<String, ServiceStats> clusterToServiceStats = new HashMap<>();
        private int index;

        public GridService(String serviceKey, String serviceName) {
            this.serviceKey = serviceKey;
            this.serviceName = serviceName;
        }

        @Override
        public int compareTo(GridService o) {
            return (serviceName + ":" + serviceKey).compareTo(o.serviceName + ":" + o.serviceKey);
        }

    }

    public Map<String, Object> waveform(String label,
        Color color,
        Color pointColor,
        List<Integer> values) {
        Map<String, Object> waveform = new HashMap<>();
        waveform.put("label", label);
        waveform.put("fillColor", "rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",0.2)");
        waveform.put("strokeColor", "rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",1)");
        waveform.put("pointColor", "rgba(" + pointColor.getRed() + "," + pointColor.getGreen() + "," + pointColor.getBlue() + ",1)");
        waveform.put("pointStrokeColor", "rgba(" + pointColor.getRed() + "," + pointColor.getGreen() + "," + pointColor.getBlue() + ",1)");
        waveform.put("pointHighlightFill", "rgba(" + pointColor.getRed() + "," + pointColor.getGreen() + "," + pointColor.getBlue() + ",1)");
        waveform.put("pointHighlightStroke", "rgba(" + pointColor.getRed() + "," + pointColor.getGreen() + "," + pointColor.getBlue() + ",1)");
        waveform.put("data", values);
        return waveform;
    }

    @Override
    public String getTitle() {
        return "Health";
    }

    String d2f(double val) {

        return String.valueOf((int) (val * 100));
    }

    private final ConcurrentMap<RingHost, UpenaEndpoints.NodeHealth> nodeHealths = Maps.newConcurrentMap();
    private final ConcurrentMap<String, Long> nodeRecency = Maps.newConcurrentMap();
    private final ConcurrentMap<RingHost, Boolean> currentlyExecuting = Maps.newConcurrentMap();

    ConcurrentMap<RingHost, UpenaEndpoints.NodeHealth> buildClusterHealth() throws Exception {

        for (final RingHost ringHost : amzaInstance.getRing("MASTER")) {
            if (currentlyExecuting.putIfAbsent(ringHost, true) == null) {
                executorService.submit(() -> {
                    try {
                        HttpRequestHelper requestHelper = buildRequestHelper(ringHost.getHost(), ringHost.getPort());
                        UpenaEndpoints.NodeHealth nodeHealth = requestHelper.executeGetRequest("/health/instance", UpenaEndpoints.NodeHealth.class, null);
                        nodeHealths.put(ringHost, nodeHealth);

                        for (UpenaEndpoints.NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                            instanceHealthHistory.compute(nannyHealth.instanceDescriptor, (instanceKey, buffer) -> {
                                if (buffer == null) {
                                    buffer = new SparseCircularHitsBucketBuffer(60, 0, 1000);
                                }
                                buffer.set(System.currentTimeMillis(), Math.max(0d, nannyHealth.serviceHealth.health));
                                return buffer;
                            });
                        }
                    } catch (Exception x) {
                        UpenaEndpoints.NodeHealth nodeHealth = new UpenaEndpoints.NodeHealth("", ringHost.getHost(), ringHost.getPort());
                        nodeHealth.health = 0.0d;
                        nodeHealth.nannyHealths = new ArrayList<>();
                        nodeHealths.put(ringHost, nodeHealth);
                        LOG.warn("Failed getting cluster health for " + ringHost + " " + x);
                    } finally {
                        nodeRecency.put(ringHost.getHost() + ":" + ringHost.getPort(), System.currentTimeMillis());
                        currentlyExecuting.remove(ringHost);
                    }
                });
            }
        }
        return nodeHealths;
    }

    HttpRequestHelper buildRequestHelper(String host, int port) {
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().setSocketTimeoutInMillis(10000).build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider()
            .createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig));
        HttpClient httpClient = httpClientFactory.createClient(host, port);
        HttpRequestHelper requestHelper = new HttpRequestHelper(httpClient, new ObjectMapper());
        return requestHelper;
    }

    String getHEXTrafficlightColor(double value, float sat) {
        //String s = Integer.toHexString(Color.HSBtoRGB(0.6f, 1f - ((float) value), sat) & 0xffffff);
        String s = Integer.toHexString(Color.HSBtoRGB((float) value / 3f, sat, 1f) & 0xffffff);
        return "000000".substring(s.length()) + s;
    }

    String trafficlightColorRGB(double value, float sat) {
        //String s = Integer.toHexString(Color.HSBtoRGB(0.6f, 1f - ((float) value), sat) & 0xffffff);
        Color color = new Color(Color.HSBtoRGB((float) value / 3f, sat, 1f));
        return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
    }

    String getHEXIdColor(double value, float sat) {
        //String s = Integer.toHexString(Color.HSBtoRGB(0.6f, 1f - ((float) value), sat) & 0xffffff);
        float hue = (float) value / 3f;
        hue = (1f / 3f) + (hue * 2);
        String s = Integer.toHexString(Color.HSBtoRGB(hue, sat, 1f) & 0xffffff);
        return "000000".substring(s.length()) + s;
    }

    String idColorRGB(double value, float sat) {
        //String s = Integer.toHexString(Color.HSBtoRGB(0.6f, 1f - ((float) value), sat) & 0xffffff);
        float hue = (float) value / 3f;
        hue = (1f / 3f) + (hue * 2);
        Color color = new Color(Color.HSBtoRGB(hue, sat, 1f));
        return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
    }

    public String renderInstanceHealth(String instanceKey) throws Exception {

        Instance instance = upenaStore.instances.get(new InstanceKey(instanceKey));
        if (instance == null) {
            return "No instance for instanceKey:" + instanceKey;
        }

        Host host = upenaStore.hosts.get(instance.hostKey);
        Cluster cluster = upenaStore.clusters.get(instance.clusterKey);
        com.jivesoftware.os.upena.shared.Service service = upenaStore.services.get(instance.serviceKey);

        Map<String, Object> data = Maps.newHashMap();
        try {
            Instance.Port port = instance.ports.get("manage");
            if (port != null) {
                HttpRequestHelper requestHelper = buildRequestHelper(host.hostName, port.port);
                HasUI hasUI = requestHelper.executeGetRequest("/manage/hasUI", HasUI.class, null);
                if (hasUI != null) {
                    List<Map<String, String>> namedUIs = new ArrayList<>();
                    for (UI ui : hasUI.uis) {
                        Instance.Port uiPort = instance.ports.get(ui.portName);
                        if (uiPort != null) {
                            Map<String, String> uiMap = new HashMap<>();
                            uiMap.put("cluster", cluster.name);
                            uiMap.put("host", host.name);
                            uiMap.put("port", String.valueOf(uiPort.port));
                            uiMap.put("service", service.name);
                            uiMap.put("instance", String.valueOf(instance.instanceId));
                            uiMap.put("name", ui.name);
                            uiMap.put("url", ui.url);
                            namedUIs.add(uiMap);
                        }
                    }
                    data.put("uis", namedUIs);
                }
            }

            // TODO fix this brute force crap
            ConcurrentMap<RingHost, UpenaEndpoints.NodeHealth> nodeHealths = buildClusterHealth();
            for (UpenaEndpoints.NodeHealth nodeHealth : nodeHealths.values()) {
                for (UpenaEndpoints.NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                    if (nannyHealth.instanceDescriptor.instanceKey.equals(instanceKey)) {
                        serviceHealth(nannyHealth, data);
                        break;
                    }
                }
            }

        } catch (Exception x) {
            LOG.debug("Failed to render instance health.", x);
        }
        return renderer.render(instanceTemplate, data);

    }

    public void serviceHealth(UpenaEndpoints.NannyHealth nannyHealth, Map<String, Object> data) throws IOException {
        if (nannyHealth == null) {
            return;
        }
        InstanceDescriptor id = nannyHealth.instanceDescriptor;
        UpenaEndpoints.ServiceHealth serviceHealth = nannyHealth.serviceHealth;

        List<String> ports = new ArrayList<>();
        for (Map.Entry<String, InstanceDescriptor.InstanceDescriptorPort> port : id.ports.entrySet()) {
            ports.add(port.getKey() + "=" + port.getValue().port);
        }
        data.put("ports", ports);
        data.put("log", nannyHealth.log);

        if (serviceHealth != null) {

            List<Map<String, String>> instanceHealths = new ArrayList<>();
            for (UpenaEndpoints.Health health : serviceHealth.healthChecks) {
                if (-Double.MAX_VALUE != health.health) {
                    Map<String, String> healthData = new HashMap<>();
                    healthData.put("color", trafficlightColorRGB(health.health, 1f));
                    healthData.put("name", String.valueOf(health.name));
                    healthData.put("status", String.valueOf(health.status));
                    healthData.put("description", String.valueOf(health.description));
                    healthData.put("resolution", String.valueOf(health.resolution));

                    long ageInMillis = System.currentTimeMillis() - health.timestamp;
                    healthData.put("age", UpenaEndpoints.shortHumanReadableUptime(ageInMillis));
                    instanceHealths.add(healthData);
                }
            }

            data.put("healths", instanceHealths);
        }

    }
}
