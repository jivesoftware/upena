package com.jivesoftware.os.upena.routing.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class HostKeyResponse {

    public final String hostKey;
    public final String message;

    @JsonCreator
    public HostKeyResponse(@JsonProperty("hostKey") String hostKey,
            @JsonProperty("message") String message) {
        this.hostKey = hostKey;
        this.message = message;
    }

    @Override
    public String toString() {
        return "HostKeyResponse{" + "hostKey=" + hostKey + ", message=" + message + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(this.hostKey);
        hash = 83 * hash + Objects.hashCode(this.message);
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
        final HostKeyResponse other = (HostKeyResponse) obj;
        if (!Objects.equals(this.hostKey, other.hostKey)) {
            return false;
        }
        if (!Objects.equals(this.message, other.message)) {
            return false;
        }
        return true;
    }
}
