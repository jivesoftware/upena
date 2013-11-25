/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Instance implements Stored<Instance>, Serializable {

    public static final String PORT_MAIN = "main";
    public static final String PORT_MANAGE = "manage";
    public static final String PORT_DEBUG = "debug";
    public static final String PORT_JMX = "jmx";
    public ClusterKey clusterKey;
    public HostKey hostKey;
    public ServiceKey serviceKey;
    public ReleaseGroupKey releaseGroupKey;
    public int instanceId;
    public boolean enabled;
    public boolean locked;
    public Map<String, Port> ports = new ConcurrentHashMap<>();

    @JsonCreator
    public Instance(@JsonProperty("clusterKey") ClusterKey clusterKey,
            @JsonProperty("hostKey") HostKey hostKey,
            @JsonProperty("serviceKey") ServiceKey serviceKey,
            @JsonProperty("releaseGroupKey") ReleaseGroupKey releaseGroupKey,
            @JsonProperty("instanceId") int instanceId,
            @JsonProperty("enabled") boolean enabled,
            @JsonProperty("locked") boolean locked) {
        this.clusterKey = clusterKey;
        this.hostKey = hostKey;
        this.serviceKey = serviceKey;
        this.releaseGroupKey = releaseGroupKey;
        this.instanceId = instanceId;
        this.enabled = enabled;
        this.locked = locked;
    }

    @Override
    public String toString() {
        return "Instance{"
                + "clusterKey=" + clusterKey
                + ", hostKey=" + hostKey
                + ", serviceKey=" + serviceKey
                + ", releaseGroupKey=" + releaseGroupKey
                + ", instanceId=" + instanceId
                + ", enabled=" + enabled
                + ", locked=" + locked
                + ", ports=" + ports
                + '}';
    }

    @Override
    public int compareTo(Instance o) {
        int c = clusterKey.compareTo(o.clusterKey);
        if (c != 0) {
            return c;
        }
        c = hostKey.compareTo(o.hostKey);
        if (c != 0) {
            return c;
        }
        c = serviceKey.compareTo(o.serviceKey);
        if (c != 0) {
            return c;
        }
        c = releaseGroupKey.compareTo(o.releaseGroupKey);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(instanceId, o.instanceId);
        if (c != 0) {
            return c;
        }
        return c;
    }

    public static class Port implements Serializable {

        public int port;
        public Map<String, String> properties = new HashMap<>();

        public Port() {
        }

        public Port(int port, Map<String, String> properties) {
            this.port = port;
            this.properties = properties;
        }

        @Override
        public String toString() {
            return "Port{" + "port=" + port + ", properties=" + properties + '}';
        }
    }
}