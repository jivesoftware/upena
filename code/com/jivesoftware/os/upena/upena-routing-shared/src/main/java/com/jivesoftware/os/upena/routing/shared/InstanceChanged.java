package com.jivesoftware.os.upena.routing.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class InstanceChanged {

    private final String hostKey;
    private final String instanceId;

    @JsonCreator
    public InstanceChanged(@JsonProperty("hostKey") String hostKey,
            @JsonProperty("instanceId") String instanceId) {
        this.hostKey = hostKey;
        this.instanceId = instanceId;
    }

    public String getHostKey() {
        return hostKey;
    }

    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public String toString() {
        return "InstanceChanged{"
                + "hostKey=" + hostKey
                + ", instanceId=" + instanceId + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.hostKey);
        hash = 71 * hash + Objects.hashCode(this.instanceId);
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
        final InstanceChanged other = (InstanceChanged) obj;
        if (!Objects.equals(this.hostKey, other.hostKey)) {
            return false;
        }
        if (!Objects.equals(this.instanceId, other.instanceId)) {
            return false;
        }
        return true;
    }
}
