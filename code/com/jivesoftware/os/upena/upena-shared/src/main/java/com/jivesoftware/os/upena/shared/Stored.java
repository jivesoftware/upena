package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "objectType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ClusterKey.class, name = "branchKey"),
    @JsonSubTypes.Type(value = Cluster.class, name = "branch"),
    @JsonSubTypes.Type(value = HostKey.class, name = "hostKey"),
    @JsonSubTypes.Type(value = Host.class, name = "host"),
    @JsonSubTypes.Type(value = ServiceKey.class, name = "serviceKey"),
    @JsonSubTypes.Type(value = Service.class, name = "service"),
    @JsonSubTypes.Type(value = ReleaseGroupKey.class, name = "userKey"),
    @JsonSubTypes.Type(value = ReleaseGroup.class, name = "user"),
    @JsonSubTypes.Type(value = InstanceKey.class, name = "instanceKey"),
    @JsonSubTypes.Type(value = Instance.class, name = "instance"),
    @JsonSubTypes.Type(value = TenantKey.class, name = "tenantKey"),
    @JsonSubTypes.Type(value = Tenant.class, name = "tenant") })
public interface Stored<S> extends Comparable<S> {
    // Marker
}
