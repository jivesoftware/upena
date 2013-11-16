package com.jivesoftware.os.upena.routing.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

public class ConnectionDescriptors {

    private final long timestamp;
    private final List<ConnectionDescriptor> connectionDescriptors;

    @JsonCreator
    public ConnectionDescriptors(@JsonProperty("timestamp") long timestamp,
            @JsonProperty("connectionDescriptors") List<ConnectionDescriptor> connectionDescriptors) {
        this.timestamp = timestamp;
        this.connectionDescriptors = connectionDescriptors;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<ConnectionDescriptor> getConnectionDescriptors() {
        return connectionDescriptors;
    }

    @Override
    public String toString() {
        return "ConnectionDescriptors{" + "timestamp=" + timestamp + ", connectionDescriptors=" + connectionDescriptors + '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + (int) (this.timestamp ^ (this.timestamp >>> 32));
        hash = 41 * hash + Objects.hashCode(this.connectionDescriptors);
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
        final ConnectionDescriptors other = (ConnectionDescriptors) obj;
        if (this.timestamp != other.timestamp) {
            return false;
        }
        if (!Objects.equals(this.connectionDescriptors, other.connectionDescriptors)) {
            return false;
        }
        return true;
    }
}
