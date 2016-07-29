package com.jivesoftware.os.upena.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptorsRequest;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptorsResponse;
import com.jivesoftware.os.routing.bird.shared.ConnectionHealth;
import com.jivesoftware.os.routing.bird.shared.InstanceConnectionHealth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author jonathan.colt
 */
public class DiscoveredRoutes {

    public final Map<ConnectionDescriptorsRequestKey, TimestampedConnectionDescriptorsResponse> discoveredConnection = new ConcurrentHashMap<>();
    public final Map<String, Map<String, Map<String, ConnectionHealth>>> from_to_Family_ConnectionHealths = new ConcurrentHashMap<>();

    public List<InstanceConnectionHealth> routesHealth(long sinceTimestampMillis) {
        List<InstanceConnectionHealth> instanceConnectionHealths = new ArrayList<>();

        for (Entry<String, Map<String, Map<String, ConnectionHealth>>> instances : from_to_Family_ConnectionHealths.entrySet()) {

            List<ConnectionHealth> connectionHealths = new ArrayList<>();
            for (Entry<String, Map<String, ConnectionHealth>> hostPorts : instances.getValue().entrySet()) {
                for (ConnectionHealth value : hostPorts.getValue().values()) {
                    if (value.timestampMillis > sinceTimestampMillis) {
                        connectionHealths.add(value);
                    }
                }
            }
            if (!connectionHealths.isEmpty()) {
                instanceConnectionHealths.add(new InstanceConnectionHealth(instances.getKey(), connectionHealths));
            }
        }
        return instanceConnectionHealths;
    }

    public void connectionHealth(InstanceConnectionHealth instanceConnectionHealth) {
        Map<String, Map<String, ConnectionHealth>> to_Family_ConnectionHealths = from_to_Family_ConnectionHealths.computeIfAbsent(
            instanceConnectionHealth.instanceId, (key) -> {
                return new ConcurrentHashMap<>();
            });

        for (ConnectionHealth connectionHealth : instanceConnectionHealth.connectionHealths) {
            Map<String, ConnectionHealth> family_ConnectionHealths = to_Family_ConnectionHealths.computeIfAbsent(
                connectionHealth.connectionDescriptor.getInstanceDescriptor().instanceKey,
                (key) -> {
                    return new ConcurrentHashMap<>();
                });

            family_ConnectionHealths.compute(connectionHealth.family, (String key, ConnectionHealth value) -> {
                if (value == null) {
                    return connectionHealth;
                } else {
                    return value.timestampMillis > connectionHealth.timestampMillis ? value : connectionHealth;
                }
            });
        }
    }

    public Map<String, Map<String, ConnectionHealth>> getConnectionHealth(String instanceId) {
        return from_to_Family_ConnectionHealths.computeIfAbsent(instanceId, (key) -> {
            return new ConcurrentHashMap<>();
        });
    }

    public void discovered(ConnectionDescriptorsRequest request, ConnectionDescriptorsResponse response) {
        ConnectionDescriptorsRequestKey connectionDescriptorsRequestKey = new ConnectionDescriptorsRequestKey(request.getTenantId(),
            request.getInstanceId(),
            request.getConnectToServiceNamed(),
            request.getPortName());
        discoveredConnection.compute(connectionDescriptorsRequestKey, (ConnectionDescriptorsRequestKey t, TimestampedConnectionDescriptorsResponse u) -> {
            if (u == null) {
                return new TimestampedConnectionDescriptorsResponse(System.currentTimeMillis(), response);
            } else {
                long now = System.currentTimeMillis();
                return (u.timestamp > now) ? u : new TimestampedConnectionDescriptorsResponse(now, response);
            }
        });
    }

    public static class ConnectionDescriptorsRequestKey {

        private final String tenantId;
        private final String instanceId;
        private final String connectToServiceNamed;
        private final String portName;

        public ConnectionDescriptorsRequestKey(String tenantId, String instanceId, String connectToServiceNamed, String portName) {
            this.tenantId = tenantId;
            this.instanceId = instanceId;
            this.connectToServiceNamed = connectToServiceNamed;
            this.portName = portName;
        }

        public String getTenantId() {
            return tenantId;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public String getConnectToServiceNamed() {
            return connectToServiceNamed;
        }

        public String getPortName() {
            return portName;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 73 * hash + Objects.hashCode(this.tenantId);
            hash = 73 * hash + Objects.hashCode(this.instanceId);
            hash = 73 * hash + Objects.hashCode(this.connectToServiceNamed);
            hash = 73 * hash + Objects.hashCode(this.portName);
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
            final ConnectionDescriptorsRequestKey other = (ConnectionDescriptorsRequestKey) obj;
            if (!Objects.equals(this.tenantId, other.tenantId)) {
                return false;
            }
            if (!Objects.equals(this.instanceId, other.instanceId)) {
                return false;
            }
            if (!Objects.equals(this.connectToServiceNamed, other.connectToServiceNamed)) {
                return false;
            }
            if (!Objects.equals(this.portName, other.portName)) {
                return false;
            }
            return true;
        }
    }

    public static class TimestampedConnectionDescriptorsResponse {

        private final long timestamp;
        private final ConnectionDescriptorsResponse response;

        public TimestampedConnectionDescriptorsResponse(long timestamp, ConnectionDescriptorsResponse response) {
            this.timestamp = timestamp;
            this.response = response;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public ConnectionDescriptorsResponse getResponse() {
            return response;
        }

    }

    public List<Route> routes() {
        List<Route> routes = new ArrayList<>();
        for (Entry<ConnectionDescriptorsRequestKey, TimestampedConnectionDescriptorsResponse> e : discoveredConnection.entrySet()) {
            ConnectionDescriptorsRequestKey key = e.getKey();
            TimestampedConnectionDescriptorsResponse value = e.getValue();
            ConnectionDescriptorsResponse response = value.getResponse();
            long timestamp = value.getTimestamp();
            routes.add(new Route(timestamp,
                key.getTenantId(), key.getInstanceId(), key.getConnectToServiceNamed(), key.getPortName(),
                response.getReturnCode(), response.getMessages(), response.getReleaseGroup(), response.getConnections()));
        }
        return routes;
    }

    static public class RouteHealths {

        private final List<InstanceConnectionHealth> routeHealths;

        @JsonCreator
        public RouteHealths(@JsonProperty("routeHealths") List<InstanceConnectionHealth> routeHealths) {
            this.routeHealths = routeHealths;
        }

        public List<InstanceConnectionHealth> getRouteHealths() {
            return routeHealths;
        }

        @Override
        public String toString() {
            return "Healths{" + "routeHealths=" + routeHealths + '}';
        }

    }

    static public class Routes {

        private final List<Route> routes;

        @JsonCreator
        public Routes(@JsonProperty("routes") List<Route> routes) {
            this.routes = routes;
        }

        public List<Route> getRoutes() {
            return routes;
        }

        @Override
        public String toString() {
            return "Routes{" + "routes=" + routes + '}';
        }

    }

    static public class Route {

        private final long timestamp;
        private final String tenantId;
        private final String instanceId;
        private final String connectToServiceNamed;
        private final String portName;
        private final int returnCode;
        private final List<String> messages;
        private final String releaseGroup;
        private final List<ConnectionDescriptor> connections;

        @JsonCreator
        public Route(@JsonProperty("timestamp") long timestamp,
            @JsonProperty("tenantId") String tenantId,
            @JsonProperty("instanceId") String instanceId,
            @JsonProperty("connectToServiceNamed") String connectToServiceNamed,
            @JsonProperty("portName") String portName,
            @JsonProperty("returnCode") int returnCode,
            @JsonProperty("messages") List<String> messages,
            @JsonProperty("releaseGroup") String releaseGroup,
            @JsonProperty("connections") List<ConnectionDescriptor> connections) {

            this.timestamp = timestamp;
            this.tenantId = tenantId;
            this.instanceId = instanceId;
            this.connectToServiceNamed = connectToServiceNamed;
            this.portName = portName;
            this.returnCode = returnCode;
            this.messages = messages;
            this.releaseGroup = releaseGroup;
            this.connections = connections;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getTenantId() {
            return tenantId;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public String getConnectToServiceNamed() {
            return connectToServiceNamed;
        }

        public String getPortName() {
            return portName;
        }

        public int getReturnCode() {
            return returnCode;
        }

        public List<String> getMessages() {
            return messages;
        }

        public String getReleaseGroup() {
            return releaseGroup;
        }

        public List<ConnectionDescriptor> getConnections() {
            return connections;
        }

        @Override
        public String toString() {
            return "Route{"
                + "timestamp=" + timestamp
                + ", tenantId=" + tenantId
                + ", instanceId=" + instanceId
                + ", connectToServiceNamed=" + connectToServiceNamed
                + ", portName=" + portName
                + ", returnCode=" + returnCode
                + ", messages=" + messages
                + ", releaseGroup=" + releaseGroup
                + ", connections=" + connections
                + '}';
        }

    }

}
