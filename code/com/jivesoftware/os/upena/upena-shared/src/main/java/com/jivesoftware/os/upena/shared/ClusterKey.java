package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClusterKey extends Key<ClusterKey> implements Stored<ClusterKey> {

    @JsonCreator
    public ClusterKey(@JsonProperty("key") String key) {
        super(key);
    }
}
