package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InstanceKey extends Key<InstanceKey> implements Stored<InstanceKey> {

    @JsonCreator
    public InstanceKey(@JsonProperty("key") String key) {
        super(key);
    }
}
