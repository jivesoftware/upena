package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jivesoftware.os.amza.shared.KeyValueFilter;
import com.jivesoftware.os.amza.shared.TimestampedValue;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class ClusterFilter implements KeyValueFilter<ClusterKey, Cluster> {

    public final String name;
    public final String description;
    public final int start;
    public final int count;
    public int hit;

    @JsonCreator
    public ClusterFilter(@JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("start") int start,
            @JsonProperty("count") int count) {
        this.name = name;
        this.description = description;
        this.start = start;
        this.count = count;
    }

    @Override
    public String toString() {
        return "ClusterFilter{"
                + "name=" + name
                + ", description=" + description
                + ", start=" + start
                + ", count=" + count
                + '}';
    }

    @Override
    public ConcurrentNavigableMap<ClusterKey, TimestampedValue<Cluster>> createCollector() {
        return new Results();
    }

    public static class Results extends ConcurrentSkipListMap<ClusterKey, TimestampedValue<Cluster>> {
    }

    @Override
    public boolean filter(ClusterKey key, Cluster value) {
        if (name != null && value.name != null) {
            if (!value.name.contains(name)) {
                return false;
            }
        }
        if (description != null && value.description != null) {
            if (!value.description.contains(description)) {
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
