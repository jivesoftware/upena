package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jivesoftware.os.amza.shared.KeyValueFilter;
import com.jivesoftware.os.amza.shared.TimestampedValue;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class TenantFilter implements KeyValueFilter<TenantKey, Tenant> {

    public final String tenantId;
    public final String description;
    public final ReleaseGroupKey releaseGroupKey;
    public final int start;
    public final int count;
    public int hit;

    @JsonCreator
    public TenantFilter(@JsonProperty("tenantId") String tenantId,
            @JsonProperty("description") String description,
            @JsonProperty("releaseGroupKey") ReleaseGroupKey releaseGroupKey,
            @JsonProperty("start") int start,
            @JsonProperty("count") int count) {
        this.tenantId = tenantId;
        this.description = description;
        this.releaseGroupKey = releaseGroupKey;
        this.start = start;
        this.count = count;
    }

    @Override
    public String toString() {
        return "TenantFilter{"
                + "tenantId=" + tenantId
                + ", description=" + description
                + ", releaseGroupKey=" + releaseGroupKey
                + ", start=" + start
                + ", count=" + count
                + '}';
    }

    @Override
    public ConcurrentNavigableMap<TenantKey, TimestampedValue<Tenant>> createCollector() {
        return new TenantFilter.Results();
    }

    public static class Results extends ConcurrentSkipListMap<TenantKey, TimestampedValue<Tenant>> {
    }

    @Override
    public boolean filter(TenantKey key, Tenant value) {
        if (tenantId != null && value.tenantId != null) {
            if (!value.tenantId.contains(tenantId)) {
                return false;
            }
        }
        if (description != null && value.description != null) {
            if (!value.description.contains(description)) {
                return false;
            }
        }
        if (releaseGroupKey != null && value.releaseGroupKey != null) {
            if (!value.releaseGroupKey.equals(releaseGroupKey)) {
                return false;
            }
        }
        hit++;
        if (hit < start) {
            return false;
        }
        if (hit > start + count) {
            return false;
        }
        return true;
    }

    @Override
    public void reset() {
        hit = 0;
    }
}