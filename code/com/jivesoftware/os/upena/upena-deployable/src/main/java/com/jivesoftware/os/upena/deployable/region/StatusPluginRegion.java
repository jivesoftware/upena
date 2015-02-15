package com.jivesoftware.os.upena.deployable.region;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.rendersnake.HtmlAttributes;
import org.rendersnake.HtmlAttributesFactory;
import org.rendersnake.HtmlCanvas;

/**
 *
 */
// soy.page.statusPluginRegion
public class StatusPluginRegion implements PageRegion<Optional<StatusPluginRegion.StatusPluginRegionInput>> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;
    private final AmzaInstance amzaInstance;
    private final UpenaStore upenaStore;
    private final UpenaService upenaService;
    private final UbaService ubaService;
    private final RingHost ringHost;

    public StatusPluginRegion(String template,
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

    public static class StatusPluginRegionInput {

        final String cluster;
        final String host;
        final String service;

        public StatusPluginRegionInput(String cluster, String host, String service) {
            this.cluster = cluster;
            this.host = host;
            this.service = service;
        }
    }

    @Override
    public String render(Optional<StatusPluginRegionInput> optionalInput) {
        Map<String, Object> data = Maps.newHashMap();

        try {
            StatusPluginRegionInput input = optionalInput.get();

            Map<String, String> filter = new HashMap<>();
            filter.put("cluster", input.cluster);
            filter.put("host", input.host);
            filter.put("service", input.service);
            data.put("filter", filter);

            List<Map<String, String>> health = new ArrayList<>();

            UpenaEndpoints.ClusterHealth clusterHealth = buildClusterHealth("health");
            ArrayList<String> lables = new ArrayList<>();
            ArrayList<Integer> values = new ArrayList<>();
            Set<String> onlyOnce = new HashSet<>();

            Map<String, Double> minClusterHealth = new HashMap<>();
            for (UpenaEndpoints.NodeHealth nodeHealth : clusterHealth.nodeHealths) {
                for (UpenaEndpoints.NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                    Double got = minClusterHealth.get(nannyHealth.instanceDescriptor.clusterKey);
                    if (got == null || got > nannyHealth.serviceHealth.health) {
                        minClusterHealth.put(nannyHealth.instanceDescriptor.clusterKey, nannyHealth.serviceHealth.health);
                    }
                }
            }

            for (UpenaEndpoints.NodeHealth nodeHealth : clusterHealth.nodeHealths) {
                for (UpenaEndpoints.NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                    boolean cshow = input.cluster.isEmpty() ? false : nannyHealth.instanceDescriptor.clusterName.contains(input.cluster);
                    boolean hshow = input.host.isEmpty() ? false : nodeHealth.host.contains(input.host);
                    boolean sshow = input.service.isEmpty() ? false : nannyHealth.instanceDescriptor.serviceName.contains(input.service);

                    if ((!input.cluster.isEmpty() == cshow) && (!input.host.isEmpty() == hshow) && (!input.service.isEmpty() == sshow)) {
                        if (!onlyOnce.contains(nodeHealth.host + ":" + nodeHealth.port)) {
                            lables.add("\"" + nodeHealth.host + ":" + nodeHealth.port + "\"");
                            values.add(Math.max(0, (int) (nodeHealth.health * 100)));
                            onlyOnce.add(nodeHealth.host + ":" + nodeHealth.port);
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

                        Double sh = nannyHealth.serviceHealth.health;
                        h.put("service", "<div title=\"" + nannyHealth.instanceDescriptor.serviceName
                            + "\" style=\"background-color:#" + getHEXTrafficlightColor(sh, 1f) + "\">"
                            + d2f(sh) + "</div>");

                        h.put("key", nannyHealth.instanceDescriptor.instanceKey);
                        h.put("name", "<span class=\"glyphicon glyphicon-zoom-in\"></span>&nbsp;&nbsp;"
                            + "<a href=\"http://" + nodeHealth.host + ":" + nannyHealth.instanceDescriptor.ports.get("manage").port + "/manage/ui\">"
                            + nannyHealth.instanceDescriptor.serviceName + " " + nannyHealth.instanceDescriptor.instanceName + "</a>");

                        HtmlCanvas hc = new HtmlCanvas();
                        serviceHealth(hc, nannyHealth);
                        h.put("details", hc.toHtml());
                        health.add(h);

                    }
                }

            }
            data.put("health", health);

            Map<String, Object> waveforms = new HashMap<>();
            waveforms.put("labels", lables);

            Color healthColor = new Color(Color.HSBtoRGB((float) Math.max(0, clusterHealth.health) / 3f, 1f, 1f));

            Map<String, Object> waveform = new HashMap<>();
            waveform.put("label", "\"My First dataset\"");
            waveform.put("fillColor", "\"rgba(" + healthColor.getRed() + "," + healthColor.getGreen() + "," + healthColor.getBlue() + ",0.8)\"");
            waveform.put("strokeColor", "\"rgba(220,220,220,1)\"");
            waveform.put("pointColor", "\"rgba(220,220,220,1)\"");
            waveform.put("pointStrokeColor", "\"#fff\"");
            waveform.put("pointHighlightFill", "\"#fff\"");
            waveform.put("pointHighlightStroke", "\"rgba(220,220,220,1)\"");
            waveform.put("data", values);

            waveforms.put("datasets", Arrays.asList(waveform));

            data.put("upenaWaveforms", waveforms);

        } catch (Exception e) {
            log.error("Unable to retrieve data", e);
        }

        return renderer.render(template, data);
    }

    @Override
    public String getTitle() {
        return "Upena Status";
    }

    private static final DecimalFormat df2 = new DecimalFormat("#.##");

    String d2f(double val) {

        return df2.format(val);
    }

    private final String shadow = "-moz-box-shadow:    2px 2px 4px 5px #ccc;"
        + "  -webkit-box-shadow: 2px 2px 4px 5px #ccc;"
        + "  box-shadow:         2px 2px 4px 5px #ccc;";

    public String getHtml() {
        try {
            HtmlCanvas h = new HtmlCanvas();

            UpenaEndpoints.ClusterHealth clusterHealth = buildClusterHealth("health");

            h.table();
            h.tr();
            h.td();

            h.div(colorStyle("background-color", clusterHealth.health, 0.75f));
            h.span(HtmlAttributesFactory.class_("dropt"));
            h.span(HtmlAttributesFactory.style("width:800px;"));

            h.content("");
            h.content(d2f(clusterHealth.health));
            h.content("");

            h._td();
            h.td();
            h.div().content(" Cluster Health ");
            h._td();
            h._tr();

            for (UpenaEndpoints.NodeHealth nodeHealth : clusterHealth.nodeHealths) {
                h.tr();
                h.td();
                h._td();
                h.td();
                addNodeHealth(h, nodeHealth);
                h._td();
                h._tr();
            }

            h._table();

            return h.toHtml();
        } catch (Exception x) {
            return "Failed building all health view.";
        }
    }

    private UpenaEndpoints.ClusterHealth buildClusterHealth(String path) throws Exception {
        UpenaEndpoints.ClusterHealth clusterHealth = new UpenaEndpoints.ClusterHealth();
        for (RingHost ringHost : new RingHost[]{
            new RingHost("soa-prime-data5.phx1.jivehosted.com", 1175),
            new RingHost("soa-prime-data6.phx1.jivehosted.com", 1175),
            new RingHost("soa-prime-data7.phx1.jivehosted.com", 1175),
            new RingHost("soa-prime-data8.phx1.jivehosted.com", 1175),
            new RingHost("soa-prime-data9.phx1.jivehosted.com", 1175),
            new RingHost("soa-prime-data10.phx1.jivehosted.com", 1175)
        }) {

            //for (RingHost ringHost : amzaInstance.getRing("MASTER")) {
            try {
                RequestHelper requestHelper = buildRequestHelper(ringHost.getHost(), ringHost.getPort());
                UpenaEndpoints.NodeHealth nodeHealth = requestHelper.executeGetRequest("/" + path + "/instance", UpenaEndpoints.NodeHealth.class,
                    null);
                clusterHealth.health = Math.min(nodeHealth.health, clusterHealth.health);
                clusterHealth.nodeHealths.add(nodeHealth);
            } catch (Exception x) {
                clusterHealth.health = 0.0d;
                UpenaEndpoints.NodeHealth nodeHealth = new UpenaEndpoints.NodeHealth(ringHost.getHost(), ringHost.getPort());
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

    private void addNodeHealth(HtmlCanvas h, UpenaEndpoints.NodeHealth nodeHealth) throws Exception {

        h.table();
        h.tr();
        h.td();
        h.div(colorStyle("background-color", nodeHealth.health, 0.75f)).content(d2f(nodeHealth.health));
        h._td();
        h.td();
        h.div().content(nodeHealth.host + ":" + nodeHealth.port);
        h._td();

        h._tr();

        if (nodeHealth.nannyHealths.isEmpty()) {
            h.tr();
            h.td();
            h._td();
            h.td();
            h.div().content("no services");
            h._td();
            h._tr();
        } else {
            for (UpenaEndpoints.NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                h.tr();
                h.td();
                h._td();
                h.td();

                addNannyHealth(h, nodeHealth, nannyHealth);

                h._td();
                h._tr();
            }
        }

        h._table();
    }

    public void serviceHealth(HtmlCanvas h, UpenaEndpoints.NannyHealth nannyHealth) throws IOException {
        InstanceDescriptor id = nannyHealth.instanceDescriptor;
        UpenaEndpoints.ServiceHealth serviceHealth = nannyHealth.serviceHealth;

        h.div(HtmlAttributesFactory.style("height:800px;overflow:auto;"));
        HtmlAttributes border = HtmlAttributesFactory.style("border: 1px solid  gray;");
        h.table(border);
        h.tr();
        h.td().content(id.clusterName);
        h.td().content(id.serviceName);
        h.td().content(id.instanceName);
        h.td().content(id.versionName);
        h._tr();
        h.tr();
        for (Map.Entry<String, InstanceDescriptor.InstanceDescriptorPort> port : id.ports.entrySet()) {
            h.td().content(port.getKey() + "=" + port.getValue().port);
        }
        h._tr();

        for (String log : nannyHealth.log) {
            h.tr();
            h.td().pre().content(log)._td();
            h._tr();
        }
        h._table();

        h.table(border);
        h.tr(HtmlAttributesFactory.style("background-color:#bbbbbb;"));
        h.td(border).content(String.valueOf("Health"));
        h.td(border).content(String.valueOf("Name"));
        h.td(border).content(String.valueOf("Status"));
        h.td(border).content(String.valueOf("Description"));
        h.td(border).content(String.valueOf("Resolution"));
        h.td(border).content(String.valueOf("Age in millis"));
        h._tr();
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
        h._table();
        h.content("");
    }

    void addNannyHealth(HtmlCanvas h, UpenaEndpoints.NodeHealth nodeHealth, UpenaEndpoints.NannyHealth nannyHealth) throws Exception {
        InstanceDescriptor id = nannyHealth.instanceDescriptor;
        UpenaEndpoints.ServiceHealth serviceHealth = nannyHealth.serviceHealth;

        h.table();
        h.tr();
        h.td();

        h.div(colorStyle("background-color", (serviceHealth == null) ? 0.0d : serviceHealth.health, 0.75f));
        h.span(HtmlAttributesFactory.class_("dropt"));
        h.span(HtmlAttributesFactory.style("width:800px;"));

        if (serviceHealth != null) {
            h.div(HtmlAttributesFactory.style("height:800px;overflow:auto;"));
            HtmlAttributes border = HtmlAttributesFactory.style("border: 1px solid  gray;");
            h.table(border);
            h.tr();
            h.td().content(id.clusterName);
            h.td().content(id.serviceName);
            h.td().content(id.instanceName);
            h.td().content(id.versionName);
            h._tr();
            h.tr();
            for (Map.Entry<String, InstanceDescriptor.InstanceDescriptorPort> port : id.ports.entrySet()) {
                h.td().content(port.getKey() + "=" + port.getValue().port);
            }
            h._tr();

            for (String log : nannyHealth.log) {
                h.tr();
                h.td().pre().content(log)._td();
                h._tr();
            }
            h._table();

            h.table(border);
            h.tr(HtmlAttributesFactory.style("background-color:#bbbbbb;"));
            h.td(border).content(String.valueOf("Health"));
            h.td(border).content(String.valueOf("Name"));
            h.td(border).content(String.valueOf("Status"));
            h.td(border).content(String.valueOf("Description"));
            h.td(border).content(String.valueOf("Resolution"));
            h.td(border).content(String.valueOf("Age in millis"));
            h._tr();
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
            h._table();
            h.content("");

        }

        h.content("");
        h.content(d2f((serviceHealth == null) ? 0.0d : serviceHealth.health));
        h.content("");

        h._td();
        h.td();
        h.div();
        h.a(HtmlAttributesFactory.href("http://" + nodeHealth.host + ":" + nannyHealth.instanceDescriptor.ports.get("manage").port + "/manage/ui"));
        h.content(id.serviceName + " " + id.instanceName);
        h.content("");
        h._td();
        h._tr();

        h.tr();
        h.td();
        h._td();

        h._tr();

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
