package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Tenant implements Stored<Tenant> {

    public final String tenantId;
    public final String description;
    public final ReleaseGroupKey releaseGroupKey;

    @JsonCreator
    public Tenant(@JsonProperty("tenantId") String tenantId,
            @JsonProperty("description") String description,
            @JsonProperty("releaseGroupKey") ReleaseGroupKey userKey) {
        this.tenantId = tenantId;
        this.description = description;
        this.releaseGroupKey = userKey;
    }

    @Override
    public String toString() {
        return "Tenant{"
                + "tenantId=" + tenantId
                + ", description=" + description
                + ", releaseGroupKey=" + releaseGroupKey
                + '}';
    }

    @Override
    public int compareTo(Tenant o) {
        return tenantId.compareTo(o.tenantId);
    }
}
