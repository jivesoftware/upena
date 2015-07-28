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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
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
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptor;
import com.jivesoftware.os.routing.bird.shared.ResponseHelper;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import com.jivesoftware.os.upena.service.DiscoveredRoutes;
import com.jivesoftware.os.upena.service.DiscoveredRoutes.Routes;
import com.jivesoftware.os.upena.service.UpenaService;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.uba.service.Nanny;
import com.jivesoftware.os.upena.uba.service.UbaService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 *
 * @author jonathan.colt
 */
@Singleton
@Path("/")
public class UpenaEndpoints {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final AmzaClusterName amzaClusterName;

    public static class AmzaClusterName {

        private final String name;

        public AmzaClusterName(String name) {
            this.name = name;
        }

    }

    private final AmzaInstance amzaInstance;
    private final UpenaStore upenaStore;
    private final UpenaService upenaService;
    private final UbaService ubaService;
    private final RingHost ringHost;
    private final HostKey ringHostKey;
    private final SoyService soyService;
    private final DiscoveredRoutes discoveredRoutes;

    public UpenaEndpoints(@Context AmzaClusterName amzaClusterName,
        @Context AmzaInstance amzaInstance,
        @Context UpenaStore upenaStore,
        @Context UpenaService upenaService,
        @Context UbaService ubaService,
        @Context RingHost ringHost,
        @Context HostKey ringHostKey,
        @Context SoyService soyService,
        @Context DiscoveredRoutes discoveredRoutes) {
        this.amzaClusterName = amzaClusterName;
        this.amzaInstance = amzaInstance;
        this.upenaStore = upenaStore;
        this.upenaService = upenaService;
        this.ubaService = ubaService;
        this.ringHost = ringHost;
        this.ringHostKey = ringHostKey;
        this.soyService = soyService;
        this.discoveredRoutes = discoveredRoutes;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public Response get(@Context HttpServletRequest httpRequest,
        @Context UriInfo uriInfo) {

        String rendered = soyService.render(httpRequest.getRemoteUser(), uriInfo.getAbsolutePath() + "propagator/download", amzaClusterName.name);
        return Response.ok(rendered).build();
    }

    @GET
    @Path("/logout")
    @Produces(MediaType.TEXT_HTML)
    public Response logout(@Context HttpServletRequest httpRequest,
        @Context UriInfo uriInfo) throws ServletException {
        httpRequest.logout();
        String rendered = soyService.render(httpRequest.getRemoteUser(), uriInfo.getAbsolutePath() + "propagator/download", amzaClusterName.name);
        return Response.ok(rendered).build();
    }

    @GET
    @Consumes("application/json")
    @Path("/instances")
    public Response getInstances(@Context UriInfo uriInfo) {
        try {
            /*
             h.table(border);
             h.tr(HtmlAttributesFactory.style("background-color:#bbbbbb;"));
             h.td(border).content(String.valueOf("Key"));
             h.td(border).content(String.valueOf("Id"));
             h.td(border).content(String.valueOf("Cluster"));
             h.td(border).content(String.valueOf("Host"));
             h.td(border).content(String.valueOf("Service"));
             h.td(border).content(String.valueOf("Release"));
             h.td(border).content(String.valueOf("Action"));
             h._tr();

             InstanceFilter filter = new InstanceFilter(null, null, null, null, null, 0, 10000);
             Map<InstanceKey, TimestampedValue<Instance>> found = upenaStore.instances.find(filter);
             for (Entry<InstanceKey, TimestampedValue<Instance>> entrySet : found.entrySet()) {
             InstanceKey key = entrySet.getKey();
             TimestampedValue<Instance> timestampedValue = entrySet.getValue();
             Instance value = timestampedValue.getValue();
             h.tr();
             h.td(HtmlAttributesFactory.style("background-color:#bbbbbb;")).content(key.getKey());
             h.td(border).content(value.instanceId);
             h.td(border).content(upenaStore.clusters.get(value.clusterKey).name);
             h.td(border).content(upenaStore.hosts.get(value.hostKey).name);
             h.td(border).content(upenaStore.services.get(value.serviceKey).name);
             h.td(border).content(upenaStore.releaseGroups.get(value.releaseGroupKey).name);
             h.td(border);
             h.button(HtmlAttributesFactory.onClick("location.href='" + uriInfo.getAbsolutePath() + "services'")).content("TODO");
             h._td();
             h._tr();
             }
             */

            return Response.ok("", MediaType.TEXT_HTML).build();
        } catch (Exception x) {
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

    @GET
    @Path("/health/check/{clusterName}/{healthy}")
    public Response getHealthCheck(@Context UriInfo uriInfo,
        @PathParam("clusterName") String clusterName,
        @PathParam("healthy") float health) {
        try {
            NodeHealth upenaHealth = buildNodeHealth();
            double minHealth = 1.0d;
            StringBuilder sb = new StringBuilder();
            for (NannyHealth nannyHealth : upenaHealth.nannyHealths) {
                if (clusterName.equals("all") || nannyHealth.instanceDescriptor.clusterName.equals(clusterName)) {
                    if (nannyHealth.serviceHealth.health < health) {
                        for (Health h : nannyHealth.serviceHealth.healthChecks) {
                            if (h.health < health) {
                                sb.append(h.toString()).append("\n");
                                if (h.health < minHealth) {
                                    minHealth = h.health;
                                }
                            }
                        }
                    }
                }
            }
            if (minHealth < health) {
                return Response.status(Response.Status.NOT_ACCEPTABLE).entity(sb.toString()).type(MediaType.TEXT_PLAIN).build();

            } else {
                return Response.ok(minHealth, MediaType.TEXT_PLAIN).build();
            }
        } catch (Exception x) {
            return Response.serverError().entity(x.toString()).type(MediaType.TEXT_PLAIN).build();
        }
    }

    @GET
    @Consumes("application/json")
    @Path("/health/instance")
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
    @Path("/routes/instances")
    public Response getInstancesRoutes() {
        try {
            return ResponseHelper.INSTANCE.jsonResponse(new Routes(discoveredRoutes.routes()));
        } catch (Exception x) {
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

    @GET
    @Consumes("application/json")
    @Path("/health/cluster")
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
        /*for (RingHost ringHost : new RingHost[]{
         new RingHost("soa-prime-data5.phx1.jivehosted.com", 1175),
         new RingHost("soa-prime-data6.phx1.jivehosted.com", 1175),
         new RingHost("soa-prime-data7.phx1.jivehosted.com", 1175),
         new RingHost("soa-prime-data8.phx1.jivehosted.com", 1175),
         new RingHost("soa-prime-data9.phx1.jivehosted.com", 1175),
         new RingHost("soa-prime-data10.phx1.jivehosted.com", 1175)
         }) { //amzaInstance.getRing("MASTER")) {*/

        for (RingHost ringHost : amzaInstance.getRing("MASTER")) {
            try {
                HttpRequestHelper requestHelper = buildRequestHelper(ringHost.getHost(), ringHost.getPort());
                String path = Joiner.on("/").join(uriInfo.getPathSegments().subList(0, uriInfo.getPathSegments().size()));
                NodeHealth nodeHealth = requestHelper.executeGetRequest("/" + path + "/instance", NodeHealth.class, null);
                clusterHealth.health = Math.min(nodeHealth.health, clusterHealth.health);
                clusterHealth.nodeHealths.add(nodeHealth);
            } catch (Exception x) {
                clusterHealth.health = 0.0d;
                NodeHealth nodeHealth = new NodeHealth("", ringHost.getHost(), ringHost.getPort());
                nodeHealth.health = 0.0d;
                nodeHealth.nannyHealths = new ArrayList<>();
                clusterHealth.nodeHealths.add(nodeHealth);
                LOG.warn("Failed getting cluster health for " + ringHost, x);
            }
        }
        return clusterHealth;
    }

    HttpRequestHelper buildRequestHelper(String host, int port) {
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().setSocketTimeoutInMillis(10000).build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider()
            .createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig));
        HttpClient httpClient = httpClientFactory.createClient(host, port);
        HttpRequestHelper requestHelper = new HttpRequestHelper(httpClient, new ObjectMapper());
        return requestHelper;
    }

    private NodeHealth buildNodeHealth() throws Exception {
        NodeHealth nodeHealth = new NodeHealth("", ringHost.getHost(), ringHost.getPort());
        for (Entry<String, Nanny> nanny : ubaService.iterateNannies()) {
            Nanny n = nanny.getValue();
            InstanceDescriptor id = n.getInstanceDescriptor();
            List<String> log = n.getDeployLog().commitedLog();
            List<String> copyLog = n.getHealthLog().commitedLog();
            ServiceHealth serviceHealth = null;
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                if (!copyLog.isEmpty()) {
                    serviceHealth = mapper.readValue(Joiner.on("").join(copyLog), ServiceHealth.class);
                    nodeHealth.health = Math.min(nodeHealth.health, serviceHealth.health);
                }
            } catch (Exception x) {
                LOG.warn("Failed parsing service health for " + id + " " + Joiner.on("").join(copyLog), x);
                nodeHealth.health = 0.0d;
                log.add("Failed to parse serviceHealth" + x.getMessage());
            }
            if (serviceHealth == null) {
                serviceHealth = new ServiceHealth();
                serviceHealth.health = -1;
            }
            String uptime = "unknown";
            if (nanny.getValue().getStartTimeMillis() > 0) {
                uptime = humanReadableUptime(System.currentTimeMillis() - nanny.getValue().getStartTimeMillis());
            }
            NannyHealth nannyHealth = new NannyHealth(uptime, id, log, serviceHealth);
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
        public String hostKey;
        public String host;
        public int port;
        public List<NannyHealth> nannyHealths = new ArrayList<>();

        public NodeHealth() {
        }

        public NodeHealth(String hostKey, String host, int port) {
            this.hostKey = hostKey;
            this.host = host;
            this.port = port;
        }

    }

    static public class NannyHealth {

        public String uptime;
        public InstanceDescriptor instanceDescriptor;
        public List<String> log;
        public ServiceHealth serviceHealth;

        public NannyHealth() {
        }

        public NannyHealth(String uptime, InstanceDescriptor instanceDescriptor, List<String> log, ServiceHealth serviceHealth) {
            this.uptime = uptime;
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
        public long checkIntervalMillis;

        @Override
        public String toString() {
            return "Health{"
                + "name=" + name
                + ", health=" + health
                + ", status=" + status
                + ", description=" + description
                + ", resolution=" + resolution
                + ", timestamp=" + timestamp
                + ", checkIntervalMillis=" + checkIntervalMillis
                + '}';
        }
    }

    public static String humanReadableLatency(long millis) {
        if (millis < 0) {
            return String.valueOf(millis);
        }

        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        millis -= TimeUnit.SECONDS.toMillis(seconds);

        StringBuilder sb = new StringBuilder(64);
        sb.append(seconds);
        sb.append(".");

        if (millis < 100) {
            sb.append('0');
        }
        if (millis < 10) {
            sb.append('0');
        }
        sb.append(millis);
        return (sb.toString());
    }

    public static String humanReadableUptime(long millis) {
        if (millis < 0) {
            return String.valueOf(millis);
        }

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        millis -= TimeUnit.SECONDS.toMillis(seconds);

        StringBuilder sb = new StringBuilder(64);
        if (hours < 10) {
            sb.append('0');
        }
        sb.append(hours);
        sb.append(":");
        if (minutes < 10) {
            sb.append('0');
        }
        sb.append(minutes);
        sb.append(":");
        if (seconds < 10) {
            sb.append('0');
        }
        sb.append(seconds);

        return (sb.toString());
    }
}
