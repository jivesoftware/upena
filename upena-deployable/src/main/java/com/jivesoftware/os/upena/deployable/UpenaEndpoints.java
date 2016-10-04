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
import com.jivesoftware.os.upena.amza.shared.AmzaInstance;
import com.jivesoftware.os.upena.amza.shared.RingHost;
import com.jivesoftware.os.upena.config.UpenaConfigStore;
import com.jivesoftware.os.upena.deployable.soy.SoyService;
import com.jivesoftware.os.upena.service.DiscoveredRoutes;
import com.jivesoftware.os.upena.service.DiscoveredRoutes.RouteHealths;
import com.jivesoftware.os.upena.service.DiscoveredRoutes.Routes;
import com.jivesoftware.os.upena.service.UpenaStore;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.PathToRepo;
import com.jivesoftware.os.upena.uba.service.Nanny;
import com.jivesoftware.os.upena.uba.service.UbaService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.io.FileUtils;

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
    private final UpenaConfigStore upenaConfigStore;
    private final UbaService ubaService;
    private final RingHost ringHost;
    private final HostKey ringHostKey;
    private final SoyService soyService;
    private final DiscoveredRoutes discoveredRoutes;
    private final PathToRepo localPathToRepo;
    private final UpenaAutoRelease autoRelease;
    private final UpenaStore upenaStore;
    private final long startupTime = System.currentTimeMillis();

    public UpenaEndpoints(@Context AmzaClusterName amzaClusterName,
        @Context AmzaInstance amzaInstance,
        @Context UpenaConfigStore upenaConfigStore,
        @Context UbaService ubaService,
        @Context RingHost ringHost,
        @Context HostKey ringHostKey,
        @Context SoyService soyService,
        @Context DiscoveredRoutes discoveredRoutes,
        @Context PathToRepo localPathToRepo,
        @Context UpenaAutoRelease autoRelease,
        @Context UpenaStore upenaStore) {
        this.amzaClusterName = amzaClusterName;
        this.amzaInstance = amzaInstance;
        this.upenaConfigStore = upenaConfigStore;
        this.ubaService = ubaService;
        this.ringHost = ringHost;
        this.ringHostKey = ringHostKey;
        this.soyService = soyService;
        this.discoveredRoutes = discoveredRoutes;
        this.localPathToRepo = localPathToRepo;
        this.autoRelease = autoRelease;
        this.upenaStore = upenaStore;
    }

    @GET
    @Path("/repo/{subResources:.*}")
    public Object pull(@PathParam("subResources") String subResources) {
        File f = new File(localPathToRepo.get(), subResources);
        try {
            if (!f.exists()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return (StreamingOutput) (OutputStream os) -> {
                try {

                    try {
                        byte[] buf = new byte[8192];
                        InputStream is = new FileInputStream(f);
                        int c = 0;
                        while ((c = is.read(buf, 0, buf.length)) > 0) {
                            os.write(buf, 0, c);
                            os.flush();
                        }
                        os.close();
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    throw new WebApplicationException(e);
                }
            };
        } catch (Exception x) {
            LOG.error("Failed to read " + f, x);
            return Response.serverError().entity(x.toString()).type(MediaType.TEXT_PLAIN).build();
        }
    }

    @PUT
    @Path("/repo/{subResources:.*}")
    public Response putFileInRepo(
        InputStream fileInputStream,
        @PathParam("subResources") String subResources) {

        File f = new File(localPathToRepo.get(), subResources);
        try {
            FileUtils.forceMkdir(f.getParentFile());
            saveFile(fileInputStream, f);
        } catch (Exception x) {
            LOG.error("Failed to write " + f, x);
            FileUtils.deleteQuietly(f);
            return Response.serverError().entity(x.toString()).type(MediaType.TEXT_PLAIN).build();
        }

        if (f.getName().endsWith(".pom")) {
            autoRelease.uploaded(f);
        }

        return Response.ok("Success").build();
    }

    // save uploaded file to a defined location on the server
    private void saveFile(InputStream uploadedInputStream, File file) {
        try (OutputStream outpuStream = new FileOutputStream(file)) {
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                outpuStream.write(bytes, 0, read);
            }
            outpuStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response get(@Context HttpServletRequest httpRequest,
        @Context UriInfo uriInfo) throws Exception {

        String rendered = soyService.render(httpRequest.getRemoteUser(), uriInfo.getAbsolutePath() + "propagator/download", amzaClusterName.name);
        return Response.ok(rendered).build();
    }

    @GET
    @Path("/logout")
    @Produces(MediaType.TEXT_HTML)
    public Response logout(@Context HttpServletRequest httpRequest,
        @Context UriInfo uriInfo) throws ServletException, Exception {
        httpRequest.logout();
        String rendered = soyService.render(httpRequest.getRemoteUser(), uriInfo.getAbsolutePath() + "propagator/download", amzaClusterName.name);
        return Response.ok(rendered).build();
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
                upenaStore.record(upenaHealth.host, "checkHealth health:" + minHealth + " < " + health, System.currentTimeMillis(), "failed", "endpoint", sb
                    .toString());
                return Response.status(Response.Status.NOT_ACCEPTABLE).entity(sb.toString()).type(MediaType.TEXT_PLAIN).build();
            } else {
                return Response.ok(minHealth, MediaType.TEXT_PLAIN).build();
            }
        } catch (Exception x) {
            LOG.error("Failed to check instance health. {} {} ", new Object[]{clusterName, health}, x);
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
            LOG.error("Failed getting instance health", x);
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
            LOG.error("Failed getting instance routes", x);
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

    @GET
    @Consumes("application/json")
    @Path("/routes/health/{sinceTimestampMillis}")
    public Response getRoutesHealth(@PathParam("sinceTimestampMillis") long sinceTimestampMillis) {
        try {
            return ResponseHelper.INSTANCE.jsonResponse(new RouteHealths(discoveredRoutes.routesHealth(sinceTimestampMillis)));
        } catch (Exception x) {
            LOG.error("Failed getting routes health", x);
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
            LOG.error("Failed getting cluster health", x);
            return ResponseHelper.INSTANCE.errorResponse("Failed building all health view.", x);
        }
    }

    private ClusterHealth buildClusterHealth(UriInfo uriInfo) throws Exception {
        ClusterHealth clusterHealth = new ClusterHealth();
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
        HttpClientConfig httpClientConfig = HttpClientConfig.newBuilder().setSocketTimeoutInMillis(10_000).build();
        HttpClientFactory httpClientFactory = new HttpClientFactoryProvider()
            .createHttpClientFactory(Arrays.<HttpClientConfiguration>asList(httpClientConfig));
        HttpClient httpClient = httpClientFactory.createClient(host, port);
        HttpRequestHelper requestHelper = new HttpRequestHelper(httpClient, new ObjectMapper());
        return requestHelper;
    }

    private NodeHealth buildNodeHealth() throws Exception {
        NodeHealth nodeHealth = new NodeHealth(ringHostKey.getKey(), ringHost.getHost(), ringHost.getPort());
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
                    serviceHealth = mapper.readValue(Joiner.on("").join(copyLog), ServiceHealth.class
                    );
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
            String uptime = "";
            if (nanny.getValue().getStartTimeMillis() > 0) {
                uptime = shortHumanReadableUptime(System.currentTimeMillis() - nanny.getValue().getStartTimeMillis());
            } else {
                uptime = ">" + shortHumanReadableUptime(System.currentTimeMillis() - startupTime);
            }

            NannyHealth nannyHealth = new NannyHealth(uptime, id, log, serviceHealth);
            if (nanny.getValue().getUnexpectedRestartTimestamp() > -1) {
                nannyHealth.unexpectedRestart = nanny.getValue().getUnexpectedRestartTimestamp();
            }

            Map<String, String> lastOverrideFetchedVersion = upenaConfigStore.changesSinceLastFetch(id.instanceKey, "override");
            Map<String, String> lastOverrideHealthFetchedVersion = upenaConfigStore.changesSinceLastFetch(id.instanceKey, "override-health");
            nannyHealth.configIsStale = lastOverrideFetchedVersion;
            nannyHealth.healthConfigIsStale = lastOverrideHealthFetchedVersion;

            nannyHealth.status = n.getStatus();
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
        public String status;
        public long unexpectedRestart = -1;
        public Map<String, String> configIsStale = new HashMap<>();
        public Map<String, String> healthConfigIsStale = new HashMap<>();

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

    public static String shortHumanReadableUptime(long millis) {
        if (millis < 0) {
            return String.valueOf(millis);
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        millis -= TimeUnit.SECONDS.toMillis(seconds);

        StringBuilder sb = new StringBuilder(64);
        if (days > 0) {
            sb.append(days + "d ");
        }
        if (hours > 0) {
            sb.append(hours + "h ");
        }
        if (minutes > 0) {
            sb.append(minutes + "m ");
        }
        if (seconds > 0) {
            sb.append(seconds + "s");
        }
        return sb.toString();
    }

}
