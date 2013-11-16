package com.jivesoftware.os.upena.routing.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class InstanceDescriptor {

    public final String clusterKey;
    public final String clusterName;
    public final String serviceKey;
    public final String serviceName;
    public final String releaseGroupKey;
    public final String releaseGroupName;
    public final String instanceKey;
    public final int instanceName;
    public final String versionName;
    public final Map<String, InstanceDescriptorPort> ports = new ConcurrentHashMap<>();
    // TODO add enable and locked

    @JsonCreator
    public InstanceDescriptor(@JsonProperty(value = "clusterKey") String clusterKey,
            @JsonProperty(value = "clusterName") String clusterName,
            @JsonProperty(value = "serviceKey") String serviceKey,
            @JsonProperty(value = "serviceName") String serviceName,
            @JsonProperty(value = "releaseGroupKey") String releaseGroupKey,
            @JsonProperty(value = "releaseGroupName") String releaseGroupName,
            @JsonProperty(value = "instanceKey") String instanceKey,
            @JsonProperty(value = "instanceName") int instanceName,
            @JsonProperty(value = "versionName") String versionName) {
        this.clusterKey = clusterKey;
        this.clusterName = clusterName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
        this.serviceKey = serviceKey;
        this.serviceName = serviceName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
        this.releaseGroupKey = releaseGroupKey;
        this.releaseGroupName = releaseGroupName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
        this.instanceKey = instanceKey;
        this.instanceName = instanceName;
        this.versionName = versionName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }

    public static class InstanceDescriptorPort {

        public final int port;

        @JsonCreator
        public InstanceDescriptorPort(@JsonProperty(value = "port") int port) {
            this.port = port;
        }

        @Override
        public String toString() {
            return "InstanceDescriptorPort{" + "port=" + port + '}';
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + this.port;
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
            final InstanceDescriptorPort other = (InstanceDescriptorPort) obj;
            if (this.port != other.port) {
                return false;
            }
            return true;
        }

    }

    @Override
    public String toString() {
        return "InstanceDescriptor{"
                + "clusterKey=" + clusterKey
                + ", clusterName=" + clusterName
                + ", serviceKey=" + serviceKey
                + ", serviceName=" + serviceName
                + ", releaseGroupKey=" + releaseGroupKey
                + ", releaseGroupName=" + releaseGroupName
                + ", instanceKey=" + instanceKey
                + ", instanceName=" + instanceName
                + ", versionName=" + versionName
                + ", ports=" + ports
                + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + Objects.hashCode(this.clusterKey);
        hash = 73 * hash + Objects.hashCode(this.clusterName);
        hash = 73 * hash + Objects.hashCode(this.serviceKey);
        hash = 73 * hash + Objects.hashCode(this.serviceName);
        hash = 73 * hash + Objects.hashCode(this.releaseGroupKey);
        hash = 73 * hash + Objects.hashCode(this.releaseGroupName);
        hash = 73 * hash + Objects.hashCode(this.instanceKey);
        hash = 73 * hash + this.instanceName;
        hash = 73 * hash + Objects.hashCode(this.versionName);
        hash = 73 * hash + Objects.hashCode(this.ports);
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
        final InstanceDescriptor other = (InstanceDescriptor) obj;
        if (!Objects.equals(this.clusterKey, other.clusterKey)) {
            return false;
        }
        if (!Objects.equals(this.clusterName, other.clusterName)) {
            return false;
        }
        if (!Objects.equals(this.serviceKey, other.serviceKey)) {
            return false;
        }
        if (!Objects.equals(this.serviceName, other.serviceName)) {
            return false;
        }
        if (!Objects.equals(this.releaseGroupKey, other.releaseGroupKey)) {
            return false;
        }
        if (!Objects.equals(this.releaseGroupName, other.releaseGroupName)) {
            return false;
        }
        if (!Objects.equals(this.instanceKey, other.instanceKey)) {
            return false;
        }
        if (this.instanceName != other.instanceName) {
            return false;
        }
        if (!Objects.equals(this.versionName, other.versionName)) {
            return false;
        }
        if (!Objects.equals(this.ports, other.ports)) {
            return false;
        }
        return true;
    }

}
