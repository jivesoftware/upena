package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Instance implements Stored<Instance> {

    public static final String PORT_MAIN = "main";
    public static final String PORT_MANAGE = "manage";
    public static final String PORT_DEBUG = "debug";
    public static final String PORT_JMX = "jmx";
    public ClusterKey clusterKey;
    public HostKey hostKey;
    public ServiceKey serviceKey;
    public ReleaseGroupKey releaseGroupKey;
    public int instanceId;
    public boolean enabled;
    public boolean locked;
    public Map<String, Port> ports = new ConcurrentHashMap<>();

    @JsonCreator
    public Instance(@JsonProperty("clusterKey") ClusterKey clusterKey,
            @JsonProperty("hostKey") HostKey hostKey,
            @JsonProperty("serviceKey") ServiceKey serviceKey,
            @JsonProperty("releaseGroupKey") ReleaseGroupKey releaseGroupKey,
            @JsonProperty("instanceId") int instanceId,
            @JsonProperty("enabled") boolean enabled,
            @JsonProperty("locked") boolean locked) {
        this.clusterKey = clusterKey;
        this.hostKey = hostKey;
        this.serviceKey = serviceKey;
        this.releaseGroupKey = releaseGroupKey;
        this.instanceId = instanceId;
        this.enabled = enabled;
        this.locked = locked;
    }

    @Override
    public String toString() {
        return "Instance{"
                + "clusterKey=" + clusterKey
                + ", hostKey=" + hostKey
                + ", serviceKey=" + serviceKey
                + ", releaseGroupKey=" + releaseGroupKey
                + ", instanceId=" + instanceId
                + ", enabled=" + enabled
                + ", locked=" + locked
                + ", ports=" + ports
                + '}';
    }

    @Override
    public int compareTo(Instance o) {
        int c = clusterKey.compareTo(o.clusterKey);
        if (c != 0) {
            return c;
        }
        c = hostKey.compareTo(o.hostKey);
        if (c != 0) {
            return c;
        }
        c = serviceKey.compareTo(o.serviceKey);
        if (c != 0) {
            return c;
        }
        c = releaseGroupKey.compareTo(o.releaseGroupKey);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(instanceId, o.instanceId);
        if (c != 0) {
            return c;
        }
        return c;
    }

    public static class Port {

        public int port;
        public Map<String, String> properties = new HashMap<>();

        public Port() {
        }

        public Port(int port, Map<String, String> properties) {
            this.port = port;
            this.properties = properties;
        }

        @Override
        public String toString() {
            return "Port{" + "port=" + port + ", properties=" + properties + '}';
        }
    }
}