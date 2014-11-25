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
            canvas.div().option(colorStyle("background-color", clusterHealth.health))
                .content("Cluster:" + clusterHealth.health)._div();

            canvas.ol();

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

        canvas.div().option(colorStyle("background-color", nodeHealth.health))
            .content(nodeHealth.host + ":" + nodeHealth.port + " " + nodeHealth.health)._div();
        canvas.ol();
        for (NannyHealth nannyHealth : nodeHealth.nannyHealths) {
            canvas.li();
            addNannyHealth(canvas, nodeHealth, nannyHealth);
            canvas._li();
        }
        canvas._ol();
    }

    private void addNannyHealth(HtmlCanvas canvas, NodeHealth nodeHealth, NannyHealth nannyHealth) throws Exception {
        InstanceDescriptor id = nannyHealth.instanceDescriptor;
        canvas.div().option(colorStyle("background-color", nannyHealth.serviceHealth.health))
            .a(HtmlAttributesFactory.href("http://" + nodeHealth.host + ":" + nannyHealth.instanceDescriptor.ports.get("manage") + "/manage/ui"))
            .content(id.clusterName + " " + id.serviceName + " " + id.instanceName + " " + id.releaseGroupName + " " + id.ports)
            ._div();
        canvas.pre().content(Joiner.on("\n").join(nannyHealth.log));

        canvas.ol();

        for (Health health : nannyHealth.serviceHealth.healthChecks) {
            canvas.li();
            addHealthCheck(canvas, health);
            canvas._li();
        }
        canvas._ol();
    }

    private void addHealthCheck(HtmlCanvas canvas, Health health) throws Exception {
        canvas.div().option(colorStyle("background-color", health.health))
            .content(health.health + " " + health.status + " " + health.name + " " + health.description + " " + health.resolution + " " + health.timestamp)
            ._div();
    }

    HtmlAttributes colorStyle(String key, double health) {
        return HtmlAttributesFactory.style(key + ":#" + getHEXTrafficlightColor(health));
    }

    String getHEXTrafficlightColor(double value) {
        return Integer.toHexString(Color.HSBtoRGB((float) value / 3f, 1f, 1f) & 0xffffff);
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
        for (RingHost ringHost : amzaInstance.getRing("master")) {
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
            List<String> log = n.getDeployLog().copyLog();
            List<String> copyLog = n.getHealthLog().copyLog();
            ServiceHealth serviceHealth = null;
            try {
                serviceHealth = new ObjectMapper().readValue(Joiner.on("").join(copyLog), ServiceHealth.class);
                nodeHealth.health = Math.min(nodeHealth.health, serviceHealth.health);
            } catch (Exception x) {
                LOG.warn("Failed parsing service health for " + id, x);
                nodeHealth.health = 0.0d;
                log.add("Failed to parse serviceHealth" + x.getMessage());
            }
            nodeHealth.nannyHealths.add(new NannyHealth(id, log, serviceHealth));

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

        public NodeHealth(String host, int port) {
            this.host = host;
            this.port = port;
        }

    }

    static public class NannyHealth {

        public InstanceDescriptor instanceDescriptor;
        public List<String> log;
        public ServiceHealth serviceHealth;

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
