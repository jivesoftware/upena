package com.jivesoftware.os.upena.deployable.region;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
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
import com.jivesoftware.os.upena.deployable.UpenaEndpoints;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ServiceKey;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
// soy.page.healthPluginRegion
public class HealthPluginRegion implements PageRegion<HealthPluginRegion.HealthPluginRegionInput> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RingHost ringHost;
    private final String template;
    private final String instanceTemplate;
    private final SoyRenderer renderer;
    private final AmzaInstance amzaInstance;
    private final UpenaStore upenaStore;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private final Map<InstanceDescriptor, SparseCircularHitsBucketBuffer> instanceHealthHistory = new ConcurrentHashMap<>();

    public HealthPluginRegion(RingHost ringHost,
        String template,
        String instanceTemplate,
        SoyRenderer renderer,
        AmzaInstance amzaInstance,
        UpenaStore upenaStore) {

        this.ringHost = ringHost;
        this.template = template;
        this.instanceTemplate = instanceTemplate;
        this.renderer = renderer;
        this.amzaInstance = amzaInstance;
        this.upenaStore = upenaStore;
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
            for (double d : rawSignal) {
                values.add(String.valueOf(d));
            }
            Map<String, Object> w = waveform(id.serviceName + "-" + id.instanceName,
                serviceColor.getOrDefault(new ServiceKey(id.serviceKey), "127,127,127"),
                1f,
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

    public Map<String, Object> waveform(String label, String color, float alpha, List<String> values) {
        Map<String, Object> waveform = new HashMap<>();
        waveform.put("label", "\"" + label + "\"");
        waveform.put("fillColor", "\"rgba(" + color + "," + String.valueOf(alpha) + ")\"");
        waveform.put("strokeColor", "\"rgba(" + color + ",1)\"");
        waveform.put("pointColor", "\"rgba(" + color + ",1)\"");
        waveform.put("pointStrokeColor", "\"rgba(" + color + ",1)\"");
        waveform.put("pointHighlightFill", "\"rgba(" + color + ",1)\"");
        waveform.put("pointHighlightStroke", "\"rgba(" + color + ",1)\"");
        waveform.put("data", values);
        return waveform;
    }

    public static class HealthPluginRegionInput implements PluginInput {

        final String cluster;
        final String host;
        final String service;

        public HealthPluginRegionInput(String cluster, String host, String service) {
            this.cluster = cluster;
            this.host = host;
            this.service = service;
        }

        public String name() {
            return "Health";
        }
    }

    public String renderLive(String user, HealthPluginRegionInput input) {

        String live = "[]";
        try {

            List<Map<String, String>> healths = new ArrayList<>();

            Map<String, Double> minHostHealth = new HashMap<>();
            for (UpenaEndpoints.NodeHealth nodeHealth : buildClusterHealth().values()) {

                for (UpenaEndpoints.NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                    if (nannyHealth.serviceHealth != null) {
                        if (nannyHealth.serviceHealth != null) {

                            double h = Math.max(0d, nannyHealth.serviceHealth.health);
                            if (nannyHealth.instanceDescriptor.enabled) {
                                minHostHealth.compute(nannyHealth.instanceDescriptor.clusterName + ":" + nodeHealth.host + ":" + nodeHealth.port,
                                    (String k, Double ev) -> {
                                        return ev == null ? nannyHealth.serviceHealth.health : Math.min(ev, nannyHealth.serviceHealth.health);
                                    });

                                healths.add(ImmutableMap.of("id", nannyHealth.instanceDescriptor.instanceKey,
                                    "color", trafficlightColorRGB(h, 1f),
                                    "text", String.valueOf((int) (h * 100)),
                                    "age", nannyHealth.uptime));
                            } else {
                                healths.add(ImmutableMap.of("id", nannyHealth.instanceDescriptor.instanceKey,
                                    "color", "64,64,64",
                                    "text", "",
                                    "age", "disabled"));
                            }
                        }
                    }
                }
            }

            for (Map.Entry<String, Double> m : minHostHealth.entrySet()) {

                String[] parts = m.getKey().split(":");
                Long recency = nodeRecency.get(parts[1] + ":" + parts[2]);
                String age = recency != null ? UpenaEndpoints.humanReadableUptime(System.currentTimeMillis() - recency) : "unknown";

                healths.add(ImmutableMap.of("id", m.getKey(),
                    "color", trafficlightColorRGB(Math.max(m.getValue(), 0d), 1f),
                    "text", m.getKey(),
                    "age", age));
            }

            live = MAPPER.writeValueAsString(healths);
        } catch (Exception x) {
            LOG.warn("failed to generate live results", x);
        }

        return live;

    }

    @Override
    public String render(String user, HealthPluginRegionInput input) {
        Map<String, Object> data = Maps.newHashMap();

        try {

            Map<ServiceKey, String> serviceColor = ServiceColorUtil.serviceKeysColor(upenaStore);

            Map<String, String> filter = new HashMap<>();
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

            ConcurrentSkipListSet<String> hosts = new ConcurrentSkipListSet<>();
            ConcurrentSkipListSet<Service> services = new ConcurrentSkipListSet<>((Service o1, Service o2) -> {
                int c = o1.serviceName.compareTo(o2.serviceName);
                if (c != 0) {
                    return c;
                }
                return o1.serviceKey.compareTo(o2.serviceKey);
            });

            Map<String, Map<String, String>> instanceHealth = new HashMap<>();

            for (UpenaEndpoints.NodeHealth nodeHealth : nodeHealths.values()) {
                if (nodeHealth.nannyHealths.isEmpty()) {
                    hosts.add("UNREACHABLE:" + nodeHealth.host + ":" + nodeHealth.port);
                }
                for (UpenaEndpoints.NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                    boolean cshow = input.cluster.isEmpty() ? false : nannyHealth.instanceDescriptor.clusterName.contains(input.cluster);
                    boolean hshow = input.host.isEmpty() ? false : nodeHealth.host.contains(input.host);
                    boolean sshow = input.service.isEmpty() ? false : nannyHealth.instanceDescriptor.serviceName.contains(input.service);

                    if ((!input.cluster.isEmpty() == cshow) && (!input.host.isEmpty() == hshow) && (!input.service.isEmpty() == sshow)) {
                        if (!hosts.contains(nannyHealth.instanceDescriptor.clusterName + ":" + nodeHealth.host + ":" + nodeHealth.port)) {
                            hosts.add(nannyHealth.instanceDescriptor.clusterName + ":" + nodeHealth.host + ":" + nodeHealth.port);
                        }
                        Service service = new Service(nannyHealth.instanceDescriptor.serviceKey, nannyHealth.instanceDescriptor.serviceName);
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

                        h.put("details", "");

                        instanceHealth.put(nannyHealth.instanceDescriptor.instanceKey, h);
                    }
                }
            }

            Map<Service, Integer> serviceIndexs = new HashMap<>();
            int serviceIndex = 0;
            for (Service service : services) {
                serviceIndexs.put(service, serviceIndex);
                serviceIndex++;
            }
            Map<String, Integer> hostIndexs = new HashMap<>();
            int hostIndex = 0;
            int uid = 0;
            List<List<Map<String, Object>>> hostRows = new ArrayList<>();
            for (String host : hosts) {
                hostIndexs.put(host, hostIndex);
                hostIndex++;
                List<Map<String, Object>> hostRow = new ArrayList<>();
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
                if (nodeHealth.nannyHealths.isEmpty()) {
                    String host = "UNREACHABLE:" + nodeHealth.host + ":" + nodeHealth.port;
                    Integer hi = hostIndexs.get(host);
                    if (hi != null) {

                        Long recency = nodeRecency.get(nodeHealth.host + ":" + nodeHealth.port);
                        String age = recency != null ? UpenaEndpoints.humanReadableUptime(System.currentTimeMillis() - recency) : "unknown";

                        float hh = (float) Math.max(0, nodeHealth.health);
                        hostRows.get(hi).get(0).put("color", "#" + getHEXTrafficlightColor(hh, 1f));
                        hostRows.get(hi).get(0).put("host", nodeHealth.host); // TODO change to hostKey
                        hostRows.get(hi).get(0).put("hostKey", nodeHealth.host); // TODO change to hostKey
                        hostRows.get(hi).get(0).put("health", host);
                        hostRows.get(hi).get(0).put("age", age);
                        hostRows.get(hi).get(0).put("uid", "uid-" + uid);
                        hostRows.get(hi).get(0).put("instanceKey", "");
                        uid++;
                    }
                }
                for (UpenaEndpoints.NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                    boolean cshow = input.cluster.isEmpty() ? false : nannyHealth.instanceDescriptor.clusterName.contains(input.cluster);
                    boolean hshow = input.host.isEmpty() ? false : nodeHealth.host.contains(input.host);
                    boolean sshow = input.service.isEmpty() ? false : nannyHealth.instanceDescriptor.serviceName.contains(input.service);

                    if ((!input.cluster.isEmpty() == cshow) && (!input.host.isEmpty() == hshow) && (!input.service.isEmpty() == sshow)) {
                        String host = nannyHealth.instanceDescriptor.clusterName + ":" + nodeHealth.host + ":" + nodeHealth.port;
                        Integer hi = hostIndexs.get(host);
                        Integer si = serviceIndexs.get(new Service(nannyHealth.instanceDescriptor.serviceKey, nannyHealth.instanceDescriptor.serviceName));
                        if (hi != null && si != null) {

                            Long recency = nodeRecency.get(nodeHealth.host + ":" + nodeHealth.port);
                            String age = recency != null ? UpenaEndpoints.humanReadableUptime(System.currentTimeMillis() - recency) : "unknown";

                            float hh = (float) Math.max(0, nodeHealth.health);
                            hostRows.get(hi).get(0).put("color", "#" + getHEXTrafficlightColor(hh, 1f));
                            hostRows.get(hi).get(0).put("host", nodeHealth.host); // TODO change to hostKey
                            hostRows.get(hi).get(0).put("hostKey", nodeHealth.host); // TODO change to hostKey
                            hostRows.get(hi).get(0).put("health", host);
                            hostRows.get(hi).get(0).put("age", age);
                            hostRows.get(hi).get(0).put("uid", "uid-" + uid);
                            hostRows.get(hi).get(0).put("instanceKey", "");
                            uid++;

                            double h = 0d;
                            if (nannyHealth.serviceHealth != null) {
                                h = nannyHealth.serviceHealth.health;
                            }
                            float sh = (float) Math.max(0, h);
                            hostRows.get(hi).get(si + 1).put("uid", "uid-" + uid);
                            uid++;
                            hostRows.get(hi).get(si + 1).put("instanceKey", nannyHealth.instanceDescriptor.instanceKey);
                            hostRows.get(hi).get(si + 1).put("clusterKey", nannyHealth.instanceDescriptor.clusterKey);
                            hostRows.get(hi).get(si + 1).put("cluster", nannyHealth.instanceDescriptor.clusterName);
                            hostRows.get(hi).get(si + 1).put("serviceKey", nannyHealth.instanceDescriptor.serviceKey);
                            hostRows.get(hi).get(si + 1).put("service", nannyHealth.instanceDescriptor.serviceName);
                            hostRows.get(hi).get(si + 1).put("releaseKey", nannyHealth.instanceDescriptor.releaseGroupKey);
                            hostRows.get(hi).get(si + 1).put("release", nannyHealth.instanceDescriptor.releaseGroupName);
                            hostRows.get(hi).get(si + 1).put("instance", String.valueOf(nannyHealth.instanceDescriptor.instanceName));
                            if (nannyHealth.instanceDescriptor.enabled) {
                                hostRows.get(hi).get(si + 1).put("color", "#" + getHEXTrafficlightColor(sh, 1f));
                                hostRows.get(hi).get(si + 1).put("health", d2f(sh));
                                hostRows.get(hi).get(si + 1).put("age", nannyHealth.uptime);
                            } else {
                                hostRows.get(hi).get(si + 1).put("color", "#404040");
                                hostRows.get(hi).get(si + 1).put("health", "");
                                hostRows.get(hi).get(si + 1).put("age", "disabled");
                            }
                            hostRows.get(hi).get(si + 1).put("link",
                                "http://" + nodeHealth.host + ":" + nannyHealth.instanceDescriptor.ports.get("manage").port + "/manage/ui");

                            List<Map<String, String>> got = (List<Map<String, String>>) hostRows.get(hi).get(si + 1).get("instances");
                            if (got == null) {
                                got = new ArrayList<>();
                                hostRows.get(hi).get(si + 1).put("instances", got);
                            }
                            got.add(instanceHealth.get(nannyHealth.instanceDescriptor.instanceKey));
                        }

                    }
                }
            }

            List<Map<String, String>> serviceData = new ArrayList<>();
            for (Service service : services) {
                Map<String, String> serviceCell = new HashMap<>();
                serviceCell.put("service", service.serviceName);
                serviceCell.put("serviceKey", service.serviceKey);
                serviceCell.put("serviceColor", serviceColor.get(new ServiceKey(service.serviceKey)));
                serviceData.add(serviceCell);
            }
            data.put("gridServices", serviceData);
            data.put("gridHost", hostRows);

        } catch (Exception e) {
            LOG.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    static class Service implements Comparable<Service> {

        String serviceKey;
        String serviceName;

        public Service(String serviceKey, String serviceName) {
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
            final Service other = (Service) obj;
            if (!Objects.equals(this.serviceKey, other.serviceKey)) {
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(Service o) {
            return (serviceName + ":" + serviceKey).compareTo(o.serviceName + ":" + o.serviceKey);
        }

    }

    public Map<String, Object> waveform(String label, Color color, List<Integer> values) {
        Map<String, Object> waveform = new HashMap<>();
        waveform.put("label", label);
        waveform.put("fillColor", "rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",0.2)");
        waveform.put("strokeColor", "rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",1)");
        waveform.put("pointColor", "rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",1)");
        waveform.put("pointStrokeColor", "rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",1)");
        waveform.put("pointHighlightFill", "rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",1)");
        waveform.put("pointHighlightStroke", "rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",1)");
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
                    try {
                        HttpRequestHelper requestHelper = buildRequestHelper(ringHost.getHost(), ringHost.getPort());
                        UpenaEndpoints.NodeHealth nodeHealth = requestHelper.executeGetRequest("/health/instance", UpenaEndpoints.NodeHealth.class, null);
                        nodeHealths.put(ringHost, nodeHealth);

                        for (UpenaEndpoints.NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                            instanceHealthHistory.compute(nannyHealth.instanceDescriptor, (instanceKey, buffer) -> {
                                if (buffer == null) {
                                    buffer = new SparseCircularHitsBucketBuffer(60, 0, 1000);
                                }
                                buffer.set(System.currentTimeMillis(), nannyHealth.serviceHealth.health);
                                return buffer;
                            });
                        }
                    } catch (Exception x) {
                        UpenaEndpoints.NodeHealth nodeHealth = new UpenaEndpoints.NodeHealth("", ringHost.getHost(), ringHost.getPort());
                        nodeHealth.health = 0.0d;
                        nodeHealth.nannyHealths = new ArrayList<>();
                        nodeHealths.put(ringHost, nodeHealth);
                        System.out.println("Failed getting cluster health for " + ringHost + " " + x);
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
                    healthData.put("age", UpenaEndpoints.humanReadableUptime(ageInMillis));
                    instanceHealths.add(healthData);
                }
            }

            data.put("healths", instanceHealths);
        }

    }
}
