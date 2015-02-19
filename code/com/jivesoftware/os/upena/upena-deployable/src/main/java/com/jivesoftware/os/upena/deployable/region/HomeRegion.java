package com.jivesoftware.os.upena.deployable.region;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.jivesoftware.os.jive.utils.http.client.HttpClient;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfig;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfiguration;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactory;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.jive.utils.http.client.rest.RequestHelper;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.deployable.UpenaEndpoints;
import com.jivesoftware.os.upena.deployable.region.HomeRegion.HomeInput;
import com.jivesoftware.os.upena.deployable.soy.SoyRenderer;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptor;
import java.awt.Color;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Map;
import org.rendersnake.HtmlAttributes;
import org.rendersnake.HtmlAttributesFactory;
import org.rendersnake.HtmlCanvas;

/**
 *
 */
public class HomeRegion implements PageRegion<HomeInput> {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();

    private final String template;
    private final SoyRenderer renderer;

    public HomeRegion(String template, SoyRenderer renderer) {
        this.template = template;
        this.renderer = renderer;
    }

    public static class HomeInput {

        final String wgetURL;
        final String upenaClusterName;

        public HomeInput(String wgetURL, String upenaClusterName) {
            this.wgetURL = wgetURL;
            this.upenaClusterName = upenaClusterName;
        }


    }

    @Override
    public String render(HomeInput input) {
        Map<String, Object> data = Maps.newHashMap();
        data.put("wgetURL", input.wgetURL);
        data.put("upenaClusterName", input.upenaClusterName);

        return renderer.render(template, data);

    }

    @Override
    public String getTitle() {
        return "Home";
    }

    private static final String roundBorder = "-moz-border-radius: 4px;"
        + "-webkit-border-radius: 4px;"
        + "border-radius: 4px;";
    private static final DecimalFormat df2 = new DecimalFormat("#.##");

    String d2f(double val) {

        return df2.format(val);
    }

    private final String shadow = "-moz-box-shadow:    2px 2px 4px 5px #ccc;"
        + "  -webkit-box-shadow: 2px 2px 4px 5px #ccc;"
        + "  box-shadow:         2px 2px 4px 5px #ccc;";

    private void clusterHeatMap(final HtmlCanvas h, UpenaEndpoints.ClusterHealth clusterHealth) throws IOException {
        h.table(HtmlAttributesFactory.style(roundBorder + "text-align:center;"
            + " border: 1px outset gray;"
            + "background-color: #" + getHEXTrafficlightColor(clusterHealth.health, 1.0F) + ";"
            + "padding: 5px;margin: 5px;"));

        int nodeCount = 0;
        h.tr();
        for (UpenaEndpoints.NodeHealth nodeHealth : clusterHealth.nodeHealths) {
            nodeCount++;
            if (nodeCount % 7 == 0) {
                h._tr();
                h.tr();
            }

            h.td(HtmlAttributesFactory.style("text-align:center; border: 1px solid gray;"
                + "background-color: #" + getHEXTrafficlightColor(nodeHealth.health, 1f) + ";"
                + "padding: 5px;margin: 5px;"));
            if (nodeHealth.nannyHealths.isEmpty()) {
                h.table(HtmlAttributesFactory.style("text-align:center; border: 1px solid gray;"
                    + "background-color: #" + getHEXTrafficlightColor(nodeHealth.health, 1f) + ";"
                    + "padding: 5px;margin: 5px;"));
                h._table();
            } else {
                for (UpenaEndpoints.NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                    if (nannyHealth.serviceHealth != null) {
                        h.table(HtmlAttributesFactory.style("text-align:center; border: 0px solid gray;"
                            + "background-color: #" + getHEXTrafficlightColor(nannyHealth.serviceHealth.health, 0.95f) + ";"
                            + "padding: 5px;margin: 5px;"));

                        if (nannyHealth.serviceHealth.healthChecks.isEmpty()) {
                            h.tr();
                            h.td();
                            InstanceDescriptor id = nannyHealth.instanceDescriptor;
                            String title = "(" + d2f(nannyHealth.serviceHealth.health) + ") for "
                                + id.clusterName + " " + id.serviceName + " " + id.instanceName + " " + id.versionName;

                            h.div(HtmlAttributesFactory.style("text-align:center;vertical-align:middle;").title(title))
                                .a(HtmlAttributesFactory
                                    .href("http://" + nodeHealth.host + ":" + nannyHealth.instanceDescriptor.ports.get("manage").port + "/manage/ui")
                                    .style(coloredStopLightButton(nannyHealth.serviceHealth.health, 1.0F)))
                                .content(nannyHealth.instanceDescriptor.serviceName).content("");
                            h._td();
                            h._tr();
                        } else {
                            for (UpenaEndpoints.Health health : nannyHealth.serviceHealth.healthChecks) {
                                h.tr();
                                h.td();
                                InstanceDescriptor id = nannyHealth.instanceDescriptor;
                                String title = "(" + d2f(health.health) + ") " + health.name + " for "
                                    + id.clusterName + " " + id.serviceName + " " + id.instanceName + " " + id.versionName;

                                h.div(HtmlAttributesFactory.style("text-align:center;vertical-align:middle;").title(title))
                                    .a(HtmlAttributesFactory
                                        .href("http://" + nodeHealth.host + ":" + nannyHealth.instanceDescriptor.ports.get("manage").port + "/manage/ui")
                                        .style(coloredStopLightButton(health.health, 1.0F))).content(health.name).content("");
                                h._td();
                                h._tr();
                            }
                        }
                        h._table();
                    }
                }
            }
            h._td();

        }
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

    private String coloredStopLightButton(double rank, float sat) {
        return roundBorder + "display: block;"
            + "  background-color: #" + getHEXTrafficlightColor(rank, sat) + ";"
            + "  padding: 3px;"
            + "  text-align: center;"
            + "  color: gray;"
            + "  border: px solid gray;";
    }


    RequestHelper buildRequestHelper(String host, int port) {
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().setSocketTimeoutInMillis(10000).build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider()
            .createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig));
        HttpClient httpClient = httpClientFactory.createClient(host, port);
        RequestHelper requestHelper = new RequestHelper(httpClient, new ObjectMapper());
        return requestHelper;
    }
}
