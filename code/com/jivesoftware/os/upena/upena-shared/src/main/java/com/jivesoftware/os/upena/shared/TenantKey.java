package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TenantKey extends Key<TenantKey> implements Stored<TenantKey> {

    @JsonCreator
    public TenantKey(@JsonProperty("key") String key) {
        super(key);
    }
}
