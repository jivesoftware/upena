package com.jivesoftware.os.upena.routing.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class InstanceDescriptorsRequest {

    public final String hostKey;

    @JsonCreator
    public InstanceDescriptorsRequest(@JsonProperty("hostKey") String hostKey) {
        this.hostKey = hostKey;
    }

    @Override
    public String toString() {
        return "InstanceDescriptorsRequest{" + "hostKey=" + hostKey + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(this.hostKey);
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
        final InstanceDescriptorsRequest other = (InstanceDescriptorsRequest) obj;
        if (!Objects.equals(this.hostKey, other.hostKey)) {
            return false;
        }
        return true;
    }
}
