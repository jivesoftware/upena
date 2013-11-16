package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HostKey extends Key<HostKey> implements Stored<HostKey> {

    @JsonCreator
    public HostKey(@JsonProperty("key") String key) {
        super(key);
    }
}
