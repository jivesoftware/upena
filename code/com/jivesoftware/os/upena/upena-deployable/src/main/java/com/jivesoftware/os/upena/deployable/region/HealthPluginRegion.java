package com.jivesoftware.os.upena.deployable.region;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.jivesoftware.os.amza.shared.AmzaInstance;
import com.jivesoftware.os.amza.shared.RingHost;
import com.jivesoftware.os.jive.utils.http.client.HttpClient;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfig;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfiguration;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactory;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.jive.utils.http.client.rest.RequestHelper;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.UpenaEndpoints;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptor;
import com.jivesoftware.os.upena.service.UpenaService;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.uba.service.UbaService;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;
import org.rendersnake.HtmlAttributes;
import org.rendersnake.HtmlAttributesFactory;
import org.rendersnake.HtmlCanvas;

/**
 *
 */
// soy.page.healthPluginRegion
public class HealthPluginRegion implements PageRegion<Optional<HealthPluginRegion.HealthPluginRegionInput>> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final AmzaInstance amzaInstance;
    private final UpenaStore upenaStore;
    private final UpenaService upenaService;
    private final UbaService ubaService;
    private final RingHost ringHost;

    public HealthPluginRegion(String template,
        SoyRenderer renderer,
        AmzaInstance amzaInstance,
        UpenaStore upenaStore,
        UpenaService upenaService,
        UbaService ubaService,
        RingHost ringHost) {
        this.template = template;
        this.renderer = renderer;
        this.amzaInstance = amzaInstance;
        this.upenaStore = upenaStore;
        this.upenaService = upenaService;
        this.ubaService = ubaService;
        this.ringHost = ringHost;
    }

    public static class HealthPluginRegionInput {

        final String cluster;
        final String host;
        final String service;

        public HealthPluginRegionInput(String cluster, String host, String service) {
            this.cluster = cluster;
            this.host = host;
            this.service = service;
        }
    }

    @Override
    public String render(Optional<HealthPluginRegionInput> optionalInput) {
        Map<String, Object> data = Maps.newHashMap();

        try {
            HealthPluginRegionInput input = optionalInput.get();

            Map<String, String> filter = new HashMap<>();
            filter.put("cluster", input.cluster);
            filter.put("host", input.host);
            filter.put("service", input.service);
            data.put("filter", filter);

            UpenaEndpoints.ClusterHealth clusterHealth = buildClusterHealth("health");

            Map<String, Double> minClusterHealth = new HashMap<>();
            for (UpenaEndpoints.NodeHealth nodeHealth : clusterHealth.nodeHealths) {
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
            ConcurrentSkipListSet<Service> services = new ConcurrentSkipListSet<>();

            Map<String, Map<String, String>> instanceHealth = new HashMap<>();

            for (UpenaEndpoints.NodeHealth nodeHealth : clusterHealth.nodeHealths) {
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

                        HtmlCanvas hc = new HtmlCanvas();
                        serviceHealth(hc, nannyHealth);
                        h.put("details", hc.toHtml());

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

            for (UpenaEndpoints.NodeHealth nodeHealth : clusterHealth.nodeHealths) {

                for (UpenaEndpoints.NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                    boolean cshow = input.cluster.isEmpty() ? false : nannyHealth.instanceDescriptor.clusterName.contains(input.cluster);
                    boolean hshow = input.host.isEmpty() ? false : nodeHealth.host.contains(input.host);
                    boolean sshow = input.service.isEmpty() ? false : nannyHealth.instanceDescriptor.serviceName.contains(input.service);

                    if ((!input.cluster.isEmpty() == cshow) && (!input.host.isEmpty() == hshow) && (!input.service.isEmpty() == sshow)) {
                        String host = nannyHealth.instanceDescriptor.clusterName + ":" + nodeHealth.host + ":" + nodeHealth.port;
                        int hi = hostIndexs.get(host);
                        int si = serviceIndexs.get(new Service(nannyHealth.instanceDescriptor.serviceKey, nannyHealth.instanceDescriptor.serviceName));

                        float hh = (float) Math.max(0, nodeHealth.health);
                        hostRows.get(hi).get(0).put("color", "#" + getHEXTrafficlightColor(hh, 1f));
                        hostRows.get(hi).get(0).put("host", nodeHealth.host); // TODO change to hostKey
                        hostRows.get(hi).get(0).put("hostKey", nodeHealth.host); // TODO change to hostKey
                        hostRows.get(hi).get(0).put("health", host);
                        hostRows.get(hi).get(0).put("uid", "uid-" + uid);
                        uid++;

                        double h = 0d;
                        if (nannyHealth.serviceHealth != null) {
                            h = nannyHealth.serviceHealth.health;
                        }
                        float sh = (float) Math.max(0, h);
                        hostRows.get(hi).get(si + 1).put("uid", "uid-" + uid);
                        uid++;
                        hostRows.get(hi).get(si + 1).put("clusterKey", nannyHealth.instanceDescriptor.clusterKey);
                        hostRows.get(hi).get(si + 1).put("cluster", nannyHealth.instanceDescriptor.clusterName);
                        hostRows.get(hi).get(si + 1).put("serviceKey", nannyHealth.instanceDescriptor.serviceKey);
                        hostRows.get(hi).get(si + 1).put("service", nannyHealth.instanceDescriptor.serviceName);
                        hostRows.get(hi).get(si + 1).put("releaseKey", nannyHealth.instanceDescriptor.releaseGroupKey);
                        hostRows.get(hi).get(si + 1).put("release", nannyHealth.instanceDescriptor.releaseGroupName);
                        hostRows.get(hi).get(si + 1).put("instance", String.valueOf(nannyHealth.instanceDescriptor.instanceName));
                        hostRows.get(hi).get(si + 1).put("color", "#" + getHEXTrafficlightColor(sh, 1f));
                        hostRows.get(hi).get(si + 1).put("health", d2f(sh) + " (" + nannyHealth.uptime + ")");
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

            List<Map<String, String>> serviceData = new ArrayList<>();
            for (Service service : services) {
                Map<String, String> serviceCell = new HashMap<>();
                serviceCell.put("service", service.serviceName);
                serviceCell.put("serviceKey", service.serviceKey);
                serviceData.add(serviceCell);
            }
            data.put("gridServices", serviceData);
            data.put("gridHost", hostRows);

        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
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
        waveform.put("label", "\"" + label + "\"");
        waveform.put("fillColor", "\"rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",0.2)\"");
        waveform.put("strokeColor", "\"rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",1)\"");
        waveform.put("pointColor", "\"rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",1)\"");
        waveform.put("pointStrokeColor", "\"rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",1)\"");
        waveform.put("pointHighlightFill", "\"rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",1)\"");
        waveform.put("pointHighlightStroke", "\"rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ",1)\"");
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

    private final String shadow = "-moz-box-shadow:    2px 2px 4px 5px #ccc;"
        + "  -webkit-box-shadow: 2px 2px 4px 5px #ccc;"
        + "  box-shadow:         2px 2px 4px 5px #ccc;";

    private UpenaEndpoints.ClusterHealth buildClusterHealth(String path) throws Exception {
        UpenaEndpoints.ClusterHealth clusterHealth = new UpenaEndpoints.ClusterHealth();
//        for (RingHost ringHost : new RingHost[]{
//            new RingHost("soa-prime-data5.phx1.jivehosted.com", 1175),
//            new RingHost("soa-prime-data6.phx1.jivehosted.com", 1175),
//            new RingHost("soa-prime-data7.phx1.jivehosted.com", 1175),
//            new RingHost("soa-prime-data8.phx1.jivehosted.com", 1175),
//            new RingHost("soa-prime-data9.phx1.jivehosted.com", 1175),
//            new RingHost("soa-prime-data10.phx1.jivehosted.com", 1175)
//        }) {

        for (RingHost ringHost : amzaInstance.getRing("MASTER")) {
            try {
                RequestHelper requestHelper = buildRequestHelper(ringHost.getHost(), ringHost.getPort());
                UpenaEndpoints.NodeHealth nodeHealth = requestHelper.executeGetRequest("/" + path + "/instance", UpenaEndpoints.NodeHealth.class,
                    null);
                clusterHealth.health = Math.min(nodeHealth.health, clusterHealth.health);
                clusterHealth.nodeHealths.add(nodeHealth);
            } catch (Exception x) {
                clusterHealth.health = 0.0d;

                UpenaEndpoints.NodeHealth nodeHealth = new UpenaEndpoints.NodeHealth("", ringHost.getHost(), ringHost.getPort());
                nodeHealth.health = 0.0d;
                nodeHealth.nannyHealths = new ArrayList<>();
                clusterHealth.nodeHealths.add(nodeHealth);
                System.out.println("Failed getting cluster health for " + ringHost + " " + x);
            }
        }
        return clusterHealth;
    }

    RequestHelper buildRequestHelper(String host, int port) {
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().setSocketTimeoutInMillis(10000).build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider()
            .createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig));
        HttpClient httpClient = httpClientFactory.createClient(host, port);
        RequestHelper requestHelper = new RequestHelper(httpClient, new ObjectMapper());
        return requestHelper;
    }

    public void serviceHealth(HtmlCanvas h, UpenaEndpoints.NannyHealth nannyHealth) throws IOException {
        InstanceDescriptor id = nannyHealth.instanceDescriptor;
        UpenaEndpoints.ServiceHealth serviceHealth = nannyHealth.serviceHealth;

        HtmlAttributes border = HtmlAttributesFactory.style("border: 1px solid  gray;");
        h.table();
        h.tr();
        for (Map.Entry<String, InstanceDescriptor.InstanceDescriptorPort> port : id.ports.entrySet()) {
            h.td().content(port.getKey() + "=" + port.getValue().port);
        }
        h._tr();
        h._table();

        h.table();
        h.tr();
        h.td().pre().content(Joiner.on("\n").join(nannyHealth.log))._td();
        h._tr();
        h._table();

        h.table(border);

        h.tr(HtmlAttributesFactory.style("background-color:#bbbbbb;"));
        h.td()
            .content(String.valueOf("Health"));
        h.td()
            .content(String.valueOf("Name"));
        h.td()
            .content(String.valueOf("Status"));
        h.td()
            .content(String.valueOf("Description"));
        h.td()
            .content(String.valueOf("Resolution"));
        h.td()
            .content(String.valueOf("Age in millis"));
        h._tr();

        if (serviceHealth
            != null) {
            for (UpenaEndpoints.Health health : serviceHealth.healthChecks) {
                if (-Double.MAX_VALUE != health.health) {
                    h.tr();
                    h.td(colorStyle("background-color", health.health, 0.0f)).content(String.valueOf(health.health));
                    h.td(border).content(String.valueOf(health.name));
                    h.td(border).content(String.valueOf(health.status));
                    h.td(border).content(String.valueOf(health.description));
                    h.td(border).content(String.valueOf(health.resolution));
                    long ageInMillis = System.currentTimeMillis() - health.timestamp;
                    double ageHealth = 0.0d;
                    if (health.checkIntervalMillis > 0) {
                        ageHealth = 1d - (ageInMillis / health.checkIntervalMillis);
                    }
                    h.td(colorStyle("background-color", ageHealth, 0.0f)).content(String.valueOf(ageInMillis));
                    h._tr();
                }
            }
        }

        h._table();
    }

    HtmlAttributes colorStyle(String key, double health, float alpha) {
        return HtmlAttributesFactory.style(key + ":#" + getHEXTrafficlightColor(health, 1.0F) + ";padding: 2px;margin: 2px;" + shadow);
    }

    String getHEXTrafficlightColor(double value, float sat) {
        //String s = Integer.toHexString(Color.HSBtoRGB(0.6f, 1f - ((float) value), sat) & 0xffffff);
        String s = Integer.toHexString(Color.HSBtoRGB((float) value / 3f, sat, 1f) & 0xffffff);
        return "000000".substring(s.length()) + s;
    }
}
