package com.jivesoftware.os.upena.deployable;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelper;
import com.jivesoftware.os.routing.bird.http.client.HttpRequestHelperUtils;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptor;
import com.jivesoftware.os.upena.amza.shared.AmzaInstance;
import com.jivesoftware.os.upena.amza.shared.RingHost;
import com.jivesoftware.os.upena.config.UpenaConfigStore;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.uba.service.Nanny;
import com.jivesoftware.os.upena.uba.service.UbaService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.UriInfo;

/**
 *
 * @author jonathan.colt
 */
public class UpenaHealth {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final ObjectMapper mapper = new ObjectMapper();
    private final AmzaInstance amzaInstance;
    private final UpenaSSLConfig upenaSSLConfig;
    private final UpenaConfigStore upenaConfigStore;
    private final UbaService ubaService;
    private final RingHost ringHost;
    private final HostKey ringHostKey;
    private final long startupTime = System.currentTimeMillis();

    public UpenaHealth(
        AmzaInstance amzaInstance,
        UpenaSSLConfig upenaSSLConfig,
        UpenaConfigStore upenaConfigStore,
        UbaService ubaService,
        RingHost ringHost,
        HostKey ringHostKey) {

        this.amzaInstance = amzaInstance;
        this.upenaSSLConfig = upenaSSLConfig;
        this.upenaConfigStore = upenaConfigStore;
        this.ubaService = ubaService;
        this.ringHost = ringHost;
        this.ringHostKey = ringHostKey;
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public UpenaHealth.ClusterHealth buildClusterHealth(UriInfo uriInfo) throws Exception {
        UpenaHealth.ClusterHealth clusterHealth = new UpenaHealth.ClusterHealth();
        for (RingHost ringHost : amzaInstance.getRing("MASTER")) {
            try {
                HttpRequestHelper requestHelper = HttpRequestHelperUtils.buildRequestHelper(upenaSSLConfig.sslEnable,
                    upenaSSLConfig.allowSelfSignedCerts, upenaSSLConfig.signer,
                    ringHost.getHost(), ringHost.getPort());
                String path = Joiner.on("/").join(uriInfo.getPathSegments().subList(0, uriInfo.getPathSegments().size()));
                UpenaHealth.NodeHealth nodeHealth = requestHelper.executeGetRequest("/" + path + "/instance", UpenaHealth.NodeHealth.class,
                    null);
                clusterHealth.health = Math.min(nodeHealth.health, clusterHealth.health);
                clusterHealth.nodeHealths.add(nodeHealth);
            } catch (Exception x) {
                clusterHealth.health = 0.0d;
                UpenaHealth.NodeHealth nodeHealth = new UpenaHealth.NodeHealth("", ringHost.getHost(), ringHost.getPort());
                nodeHealth.health = 0.0d;
                nodeHealth.nannyHealths = new ArrayList<>();
                clusterHealth.nodeHealths.add(nodeHealth);
                LOG.warn("Failed getting cluster health for " + ringHost, x);
            }
        }
        return clusterHealth;
    }

    public NodeHealth buildNodeHealth() throws Exception {
        NodeHealth nodeHealth = new NodeHealth(ringHostKey.getKey(), ringHost.getHost(), ringHost.getPort());
        for (Map.Entry<String, Nanny> nanny : ubaService.iterateNannies()) {
            Nanny n = nanny.getValue();
            InstanceDescriptor id = n.getInstanceDescriptor();
            List<String> log = n.getDeployLog().commitedLog();
            List<String> copyLog = n.getHealthLog().commitedLog();
            ServiceHealth serviceHealth = null;
            try {

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
