package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReleaseGroupKey extends Key<ReleaseGroupKey> implements Stored<ReleaseGroupKey> {

    @JsonCreator
    public ReleaseGroupKey(@JsonProperty("key") String key) {
        super(key);
    }
}
