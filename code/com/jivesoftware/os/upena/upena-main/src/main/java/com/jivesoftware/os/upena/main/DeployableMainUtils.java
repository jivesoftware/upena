package com.jivesoftware.os.upena.main;

import com.jivesoftware.os.jive.utils.logger.MetricLogger;
import com.jivesoftware.os.jive.utils.logger.MetricLoggerFactory;
import com.jivesoftware.os.upena.routing.shared.TenantRoutingProvider;
import java.util.Arrays;

public class DeployableMainUtils {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final TenantRoutingProvider tenantRoutingConnectionDescriptorsProvider;
    private final ComponentHealthCheckProvider componentHealthCheckProvider;
    private final KillSwitchProvider killSwitchProvider;
    private final DeployableFoundationService initializer;

    private String clusterName = "unnamedCluster";
    private String hostName = "unnamedHostName";
    private String serviceName = "unnamedServiceName";
    private String instanceName = "unnamedInstanceName";
    private int primaryPort = -1;
    private int managePort = -1;

    public DeployableMainUtils(TenantRoutingProvider tenantRoutingConnectionDescriptorsProvider,
            ComponentHealthCheckProvider componentHealthCheckProvider,
            KillSwitchProvider killSwitchProvider,
            DeployableFoundationService initializer) {
        this.tenantRoutingConnectionDescriptorsProvider = tenantRoutingConnectionDescriptorsProvider;
        this.componentHealthCheckProvider = componentHealthCheckProvider;
        this.killSwitchProvider = killSwitchProvider;
        this.initializer = initializer;
    }

    public boolean build() {
        String name = initializer.getClass().getSimpleName();
        banneredOneLiner("Starting Service '" + name + "'");
        long time = System.currentTimeMillis();

        ComponentHealthCheck componentHealthCheck = componentHealthCheckProvider.create("'" + name + "'service initialization", true);
        try {
            initializer.initialize(tenantRoutingConnectionDescriptorsProvider, componentHealthCheckProvider, killSwitchProvider);
            componentHealthCheck.setHealthy("'" + name + "' service is initialized.");
            banneredOneLiner("'" + name + "' service started. Elapse:" + (System.currentTimeMillis() - time));
            return true;
        } catch (Exception x) {
            componentHealthCheck.setUnhealthy("Failed to initialize service '" + name + "'.", x);
            LOG.error("Failed to initialize service '" + name + "'.", x);
            return false;
        }
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public int getPrimaryPort() {
        return primaryPort;
    }

    public void setPrimaryPort(int primaryPort) {
        this.primaryPort = primaryPort;
    }

    public int getManagePort() {
        return managePort;
    }

    public void setManagePort(int managePort) {
        this.managePort = managePort;
    }

    void banneredOneLiner(String message) {
        System.out.println(pad("-", "", "-", '-', 100));
        System.out.println(pad("|", "", "|", ' ', 100));
        System.out.println(pad("|", "      " + message, "|", ' ', 100));
        System.out.println(pad("|", "", "|", ' ', 100));
        System.out.println(pad("-", "", "-", '-', 100));
    }

    void startedUpBanner(String instanceKey) {
        System.out.println(pad("-", "", "-", '-', 100));
        System.out.println(pad("|", "", "|", ' ', 100));
        System.out.println(pad("|", "      Service INSTANCEID:" + instanceKey, "|", ' ', 100));
        System.out.println(pad("|", "", "|", ' ', 100));
        System.out.println(pad("|", "      Service CLUSTER:" + clusterName, "|", ' ', 100));
        System.out.println(pad("|", "         Service HOST:" + hostName, "|", ' ', 100));
        System.out.println(pad("|", "      Service SERVICE:" + serviceName, "|", ' ', 100));
        System.out.println(pad("|", "     Service INSTANCE:" + instanceName, "|", ' ', 100));
        System.out.println(pad("|", "         Primary PORT:" + primaryPort, "|", ' ', 100));
        System.out.println(pad("|", "          Manage PORT:" + managePort, "|", ' ', 100));
        System.out.println(pad("|", "", "|", ' ', 100));
        System.out.println(pad("|", "        curl " + hostName + ":" + managePort + "/manage/help", "|", ' ', 100));
        System.out.println(pad("|", "", "|", ' ', 100));
        System.out.println(pad("-", "", "-", '-', 100));
    }

    String pad(String prefic, String string, String postFix, char pad, int totalLength) {
        if (string.length() >= totalLength) {
            return string;
        }
        char[] padding = new char[totalLength - string.length()];
        Arrays.fill(padding, pad);
        return prefic + string + new String(padding) + postFix;
    }

}
