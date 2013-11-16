package com.jivesoftware.os.upena.routing.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class InstanceDescriptorsResponse {

    public static final String PORT_MAIN = "main";
    public static final String PORT_MANAGE = "manage";
    public static final String PORT_DEBUG = "debug";
    public static final String PORT_JMX = "jmx";
    public final String requestingHostId;
    public final boolean decommisionRequestingHost;
    public final List<InstanceDescriptor> instanceDescriptors = new ArrayList<>();

    @JsonCreator
    public InstanceDescriptorsResponse(@JsonProperty("requestingHostId") String requestingHostId,
            @JsonProperty("decommisionRequestingHost") boolean decommisionRequestingHost) {
        this.requestingHostId = requestingHostId;
        this.decommisionRequestingHost = decommisionRequestingHost;
    }

    @Override
    public String toString() {
        return "InstanceDescriptorsResponse{"
                + "requestingHostId=" + requestingHostId
                + ", decommisionRequestingHost=" + decommisionRequestingHost
                + ", instanceDescriptors=" + instanceDescriptors + '}';
    }
}