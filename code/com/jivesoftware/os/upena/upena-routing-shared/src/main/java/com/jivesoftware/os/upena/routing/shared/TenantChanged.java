package com.jivesoftware.os.upena.routing.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class TenantChanged {

    private final String tenantId;

    @JsonCreator
    public TenantChanged(@JsonProperty("tenantId") String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }

    @Override
    public String toString() {
        return "TenantChanged{" + "tenantId=" + tenantId + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.tenantId);
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
        final TenantChanged other = (TenantChanged) obj;
        if (!Objects.equals(this.tenantId, other.tenantId)) {
            return false;
        }
        return true;
    }
}
