package com.jivesoftware.os.upena.routing.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.Objects;

public class ConnectionDescriptor {

    private final String host;
    private final int port;
    private final Map<String, String> properties;

    @JsonCreator
    public ConnectionDescriptor(@JsonProperty("host") String host,
            @JsonProperty("port") int port,
            @JsonProperty("properties") Map<String, String> properties) {
        this.host = host;
        this.port = port;
        this.properties = properties;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "ConnectionDescriptor{"
                + "host=" + host
                + ", port=" + port
                + ", properties=" + properties
                + '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + Objects.hashCode(this.host);
        hash = 31 * hash + this.port;
        hash = 31 * hash + Objects.hashCode(this.properties);
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
        final ConnectionDescriptor other = (ConnectionDescriptor) obj;
        if (!Objects.equals(this.host, other.host)) {
            return false;
        }
        if (this.port != other.port) {
            return false;
        }
        if (!Objects.equals(this.properties, other.properties)) {
            return false;
        }
        return true;
    }
}
