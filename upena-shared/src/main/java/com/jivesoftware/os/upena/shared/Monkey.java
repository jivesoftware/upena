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

@JsonIgnoreProperties(ignoreUnknown = true)
public class Monkey implements Stored<Monkey>, Serializable {

    public final boolean enabled;
    public final ClusterKey clusterKey;
    public final HostKey hostKey;
    public final ServiceKey serviceKey;
    public final ChaosStrategyKey strategyKey;

    @JsonCreator
    public Monkey(@JsonProperty("enabled") boolean enabled,
                  @JsonProperty("clusterKey") ClusterKey clusterKey,
                  @JsonProperty("hostKey") HostKey hostKey,
                  @JsonProperty("serviceKey") ServiceKey serviceKey,
                  @JsonProperty("strategyKey") ChaosStrategyKey strategyKey) {
        this.enabled = enabled;
        this.clusterKey = clusterKey;
        this.hostKey = hostKey;
        this.serviceKey = serviceKey;
        this.strategyKey = strategyKey;
    }

    @Override
    public String toString() {
        return "Monkey{"
                + "enabled=" + enabled
                + ", clusterKey=" + clusterKey
                + ", hostKey=" + hostKey
                + ", serviceKey=" + serviceKey
                + ", strategyKey=" + strategyKey
                + '}';
    }

    @Override
    public int compareTo(Monkey o) {
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
        c = strategyKey.compareTo(o.strategyKey);
        if (c != 0) {
            return c;
        }
        return c;
    }

}
