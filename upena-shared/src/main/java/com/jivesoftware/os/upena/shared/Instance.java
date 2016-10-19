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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Instance implements Stored<Instance>, Serializable {

    public static final String PORT_MAIN = "main";
    public static final String PORT_MANAGE = "manage";
    public static final String PORT_DEBUG = "debug";
    public static final String PORT_JMX = "jmx";
    public final ClusterKey clusterKey;
    public final HostKey hostKey;
    public final ServiceKey serviceKey;
    public final ReleaseGroupKey releaseGroupKey;
    public final int instanceId;
    public boolean enabled;
    public final boolean locked;
    public final String publicKey;
    public long restartTimestampGMTMillis = -1;
    public final Map<String, Port> ports;

    @JsonCreator
    public Instance(@JsonProperty("clusterKey") ClusterKey clusterKey,
        @JsonProperty("hostKey") HostKey hostKey,
        @JsonProperty("serviceKey") ServiceKey serviceKey,
        @JsonProperty("releaseGroupKey") ReleaseGroupKey releaseGroupKey,
        @JsonProperty("instanceId") int instanceId,
        @JsonProperty("enabled") boolean enabled,
        @JsonProperty("locked") boolean locked,
        @JsonProperty("publicKey") String publicKey,
        @JsonProperty("restartTimestampGMTMillis") long restartTimestampGMTMillis,
        @JsonProperty("ports") Map<String, Port> ports
    ) {
        this.clusterKey = clusterKey;
        this.hostKey = hostKey;
        this.serviceKey = serviceKey;
        this.releaseGroupKey = releaseGroupKey;
        this.instanceId = instanceId;
        this.enabled = enabled;
        this.locked = locked;
        this.publicKey = publicKey;
        this.restartTimestampGMTMillis = restartTimestampGMTMillis;
        this.ports = ports;
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
            + ", publicKey=" + publicKey
            + ", restartTimestampGMTMillis=" + restartTimestampGMTMillis
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

        public boolean sslEnabled;
        public int port;
        public Map<String, String> properties = new HashMap<>();

        public Port() {
        }

        public Port(boolean sslEnabled, int port, Map<String, String> properties) {
            this.sslEnabled = sslEnabled;
            this.port = port;
            this.properties = properties;
        }

        @Override
        public String toString() {
            return "Port{" + "sslEnabled=" + sslEnabled + ", port=" + port + ", properties=" + properties + '}';
        }

    }
}
