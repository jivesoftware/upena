/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.upena.deployable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.jivesoftware.os.amza.shared.AmzaInstance;
import com.jivesoftware.os.amza.shared.RingHost;
import com.jivesoftware.os.jive.utils.http.client.HttpClient;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfig;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfiguration;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactory;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.jive.utils.http.client.rest.RequestHelper;
import com.jivesoftware.os.jive.utils.jaxrs.util.ResponseHelper;
import com.jivesoftware.os.jive.utils.logger.MetricLogger;
import com.jivesoftware.os.jive.utils.logger.MetricLoggerFactory;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptor;
import com.jivesoftware.os.upena.service.UpenaService;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.uba.service.Nanny;
import com.jivesoftware.os.upena.uba.service.UbaService;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.rendersnake.HtmlAttributes;
import org.rendersnake.HtmlAttributesFactory;
import org.rendersnake.HtmlCanvas;

/**
 *
 * @author jonathan.colt
 */
@Singleton
@Path("/upena/health")
public class UpenaHealthEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final AmzaInstance amzaInstance;
    private final UpenaStore upenaStore;
    private final UpenaService upenaService;
    private final UbaService ubaService;
    private final RingHost ringHost;

    public UpenaHealthEndpoints(@Context AmzaInstance amzaInstance,
        @Context UpenaStore upenaStore,
        @Context UpenaService upenaService,
        @Context UbaService ubaService,
        @Context RingHost ringHost) {
        this.amzaInstance = amzaInstance;
        this.upenaStore = upenaStore;
        this.upenaService = upenaService;
        this.ubaService = ubaService;
        this.ringHost = ringHost;
    }

    @GET
    @Consumes("application/json")
    @Path("/ui")
    public Response getHtml(@Context UriInfo uriInfo) {
        try {
            final HtmlCanvas canvas = new HtmlCanvas();
            canvas.html();
            canvas.body();

            ClusterHealth clusterHealth = buildClusterHealth(uriInfo);
            canvas.table(HtmlAttributesFactory.style("border: 1px solid  gray;padding:10px;background-color:#"
                + getHEXTrafficlightColor(clusterHealth.health)));
            canvas.tr();
            for (NodeHealth nodeHealth : clusterHealth.nodeHealths) {
                canvas.td(HtmlAttributesFactory.style("text-align:center; border: 1px solid  gray;padding:10px;background-color:#"
                    + getHEXTrafficlightColor(nodeHealth.health)));
                for (NannyHealth nannyHealth : nodeHealth.nannyHealths) {
                    if (nannyHealth.serviceHealth != null) {
                        canvas.table(HtmlAttributesFactory.style("border: 1px solid  gray;padding:10px;background-color:#"
                            + getHEXTrafficlightColor(nannyHealth.serviceHealth.health)));
                        for (Health health : nannyHealth.serviceHealth.healthChecks) {
                            canvas.tr();
                            canvas.td(HtmlAttributesFactory.style("border: 1px solid  gray;"));
                            int size = 10 + (int) (64 * (1d - health.health));
                            canvas.div(HtmlAttributesFactory.style("background-color: #" + getHEXTrafficlightColor(health.health) + ";"
                                + "width: " + size + "px;height: " + size + "px;"))
                                .a(HtmlAttributesFactory
                                    .href("http://" + nodeHealth.host + ":" + nannyHealth.instanceDescriptor.ports.get("manage").port + "/manage/ui")
                                    .style("alt:" + "booya" + ";display: block;width:100%;height:100%;text-decoration: none;")).content("")
                                .content("");
                            canvas._td();
                            canvas._tr();
                        }
                        canvas._table();
                    }
                }
                canvas._td();
            }
            canvas._tr();
            canvas._table();
            canvas.br();

            canvas.div()
                .div(colorStyle("background-color", clusterHealth.health))
                .content(String.valueOf(clusterHealth.health))
                .h1()
                .content("Cluster")
                .content("");

            canvas.ol(HtmlAttributesFactory.style("list-style-type:none;"));

            for (NodeHealth nodeHealth : clusterHealth.nodeHealths) {
                canvas.li();
                addNodeHealth(canvas, nodeHealth);
                canvas._li();
            }
            canvas._ol();

            canvas._body();
            canvas._html();

            return Response.ok(canvas.toHtml(), MediaType.TEXT_HTML).build();
        } catch (Exception x) {
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

    private void addNodeHealth(HtmlCanvas canvas, NodeHealth nodeHealth) throws Exception {

        canvas.div()
            .div(colorStyle("background-color", nodeHealth.health))
            .content(String.valueOf(nodeHealth.health))
            .h2()
            .content(nodeHealth.host + ":" + nodeHealth.port)
            .content("");

        canvas.ol(HtmlAttributesFactory.style("list-style-type:none;"));
        for (NannyHealth nannyHealth : nodeHealth.nannyHealths) {
            canvas.li();
            addNannyHealth(canvas, nodeHealth, nannyHealth);
            canvas._li();
        }
        canvas._ol();
    }

    private void addNannyHealth(HtmlCanvas canvas, NodeHealth nodeHealth, NannyHealth nannyHealth) throws Exception {
        InstanceDescriptor id = nannyHealth.instanceDescriptor;
        ServiceHealth serviceHealth = nannyHealth.serviceHealth;

        canvas.div()
            .div(colorStyle("background-color", (serviceHealth == null) ? 0.0d : serviceHealth.health))
            .content(String.valueOf((serviceHealth == null) ? 0.0d : serviceHealth.health))
            .h3()
            .a(HtmlAttributesFactory.href("http://" + nodeHealth.host + ":" + nannyHealth.instanceDescriptor.ports.get("manage").port + "/manage/ui"))
            .content(id.clusterName + " " + id.serviceName + " " + id.instanceName + " " + id.versionName)
            ._h3()
            .content("");
            //

        StringBuilder sb = new StringBuilder();
        for (Entry<String, InstanceDescriptor.InstanceDescriptorPort> port : id.ports.entrySet()) {
            sb.append(port.getKey()).append("=").append(port.getValue().port).append(" ");
        }

        canvas.ol(HtmlAttributesFactory.style("list-style-type:none;"));
        canvas.pre().content(sb.toString());
        canvas.pre().content(Joiner.on("\n").join(nannyHealth.log));

        if (serviceHealth != null) {
            

            HtmlAttributes border = HtmlAttributesFactory.style("border: 1px solid  gray;");
            canvas.table(border);
            canvas.tr(HtmlAttributesFactory.style("background-color:#bbbbbb;"));
            canvas.td(border).content(String.valueOf("Health"));
            canvas.td(border).content(String.valueOf("Name"));
            canvas.td(border).content(String.valueOf("Status"));
            canvas.td(border).content(String.valueOf("Description"));
            canvas.td(border).content(String.valueOf("Resolution"));
            canvas.td(border).content(String.valueOf("Age in millis"));
            canvas._tr();
            for (Health health : serviceHealth.healthChecks) {
                if (-Double.MAX_VALUE != health.health) {
                    canvas.tr();
                    canvas.td(colorStyle("background-color", health.health)).content(String.valueOf(health.health));
                    canvas.td(border).content(String.valueOf(health.name));
                    canvas.td(border).content(String.valueOf(health.status));
                    canvas.td(border).content(String.valueOf(health.description));
                    canvas.td(border).content(String.valueOf(health.resolution));
                    canvas.td(border).content(String.valueOf(System.currentTimeMillis() - health.timestamp));
                    canvas._tr();
                }
            }
            canvas._table();
           
        }
        canvas._ol();
    }

    HtmlAttributes colorStyle(String key, double health) {
        return HtmlAttributesFactory.style(key + ":#" + getHEXTrafficlightColor(health) + ";float: left;padding: 8px;margin: 8px;");
    }

    String getHEXTrafficlightColor(double value) {
        String s = Integer.toHexString(Color.HSBtoRGB((float) value / 3f, 1f, 1f) & 0xffffff);
        return "000000".substring(s.length()) + s;
    }

    @GET
    @Consumes("application/json")
    @Path("/instance")
    public Response getInstanceHealth() {
        try {
            NodeHealth upenaHealth = buildNodeHealth();
            return ResponseHelper.INSTANCE.jsonResponse(upenaHealth);
        } catch (Exception x) {
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

    @GET
    @Consumes("application/json")
    @Path("/cluster")
    public Response getClusterHealth(@Context UriInfo uriInfo) {
        try {
            ClusterHealth clusterHealth = buildClusterHealth(uriInfo);
            return ResponseHelper.INSTANCE.jsonResponse(clusterHealth);
        } catch (Exception x) {
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

    private ClusterHealth buildClusterHealth(UriInfo uriInfo) throws Exception {
        ClusterHealth clusterHealth = new ClusterHealth();
        for (RingHost ringHost : amzaInstance.getRing("MASTER")) {
            try {
                RequestHelper requestHelper = buildRequestHelper(ringHost.getHost(), ringHost.getPort());
                String path = Joiner.on("/").join(uriInfo.getPathSegments().subList(0, uriInfo.getPathSegments().size() - 1));
                NodeHealth nodeHealth = requestHelper.executeGetRequest("/" + path + "/instance", NodeHealth.class, null);
                clusterHealth.health = Math.min(nodeHealth.health, clusterHealth.health);
                clusterHealth.nodeHealths.add(nodeHealth);
            } catch (Exception x) {
                clusterHealth.health = 0.0d;
                LOG.warn("Failed getting cluster health for " + ringHost, x);
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

    private NodeHealth buildNodeHealth() {
        NodeHealth nodeHealth = new NodeHealth(ringHost.getHost(), ringHost.getPort());
        for (Entry<String, Nanny> nanny : ubaService.iterateNannies()) {
            Nanny n = nanny.getValue();
            InstanceDescriptor id = n.getInstanceDescriptor();
            List<String> log = n.getDeployLog().commitedLog();
            List<String> copyLog = n.getHealthLog().commitedLog();
            ServiceHealth serviceHealth = null;
            try {
                serviceHealth = new ObjectMapper().readValue(Joiner.on("").join(copyLog), ServiceHealth.class);
                nodeHealth.health = Math.min(nodeHealth.health, serviceHealth.health);
            } catch (Exception x) {
                LOG.warn("Failed parsing service health for " + id + " " + Joiner.on("").join(copyLog), x);
                nodeHealth.health = 0.0d;
                log.add("Failed to parse serviceHealth" + x.getMessage());
            }
            NannyHealth nannyHealth = new NannyHealth(id, log, serviceHealth);
            nodeHealth.nannyHealths.add(nannyHealth);

        }
        return nodeHealth;
    }

    static public class ClusterHealth {

        public double health = 1d;
        public List<NodeHealth> nodeHealths = new ArrayList<>();
    }

    static public class NodeHealth {

        public double health = 1d;
        public String host;
        public int port;
        public List<NannyHealth> nannyHealths = new ArrayList<>();

        public NodeHealth() {
        }

        public NodeHealth(String host, int port) {
            this.host = host;
            this.port = port;
        }

    }

    static public class NannyHealth {

        public InstanceDescriptor instanceDescriptor;
        public List<String> log;
        public ServiceHealth serviceHealth;

        public NannyHealth() {
        }

        public NannyHealth(InstanceDescriptor instanceDescriptor, List<String> log, ServiceHealth serviceHealth) {
            this.instanceDescriptor = instanceDescriptor;
            this.log = log;
            this.serviceHealth = serviceHealth;
        }

    }

    static public class ServiceHealth {

        public double health = 1.0d;
        public List<Health> healthChecks = new ArrayList<>();
    }

    static public class Health {

        public String name;
        public double health;
        public String status;
        public String description;
        public String resolution;
        public long timestamp;
    }

}
