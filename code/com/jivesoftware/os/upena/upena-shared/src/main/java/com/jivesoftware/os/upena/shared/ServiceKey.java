package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceKey extends Key<ServiceKey> implements Stored<ServiceKey> {

    @JsonCreator
    public ServiceKey(@JsonProperty("key") String key) {
        super(key);
    }
}
