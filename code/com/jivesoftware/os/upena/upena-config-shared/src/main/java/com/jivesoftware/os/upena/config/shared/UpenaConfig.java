package com.jivesoftware.os.upena.config.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class UpenaConfig {

    public final String context;
    public final String instanceKey;
    public final Map<String, String> properties;

    @JsonCreator
    public UpenaConfig(@JsonProperty("context") String context,
            @JsonProperty("instanceKey") String instanceKey,
            @JsonProperty("properties") Map<String, String> properties) {
        this.context = context;
        this.instanceKey = instanceKey;
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "UpenaConfig{"
                + "context=" + context
                + ", instanceKey=" + instanceKey
                + ", properties=" + properties
                + '}';
    }
}