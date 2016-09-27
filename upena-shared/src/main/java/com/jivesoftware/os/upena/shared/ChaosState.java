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
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChaosState implements Stored<ChaosState>, Serializable {

    public final ServiceKey serviceKey;
    public final long enableAfterTime;
    public final long disableAfterTime;
    public final Map<InstanceKey, Set<InstanceKey>> instanceRoutes;
    public final Map<String, String> properties;

    @JsonCreator
    public ChaosState(@JsonProperty("serviceKey") ServiceKey serviceKey,
                      @JsonProperty("enableAfter") long enableAfterTime,
                      @JsonProperty("disableAfter") long disableAfterTime,
                      @JsonProperty("instanceRoutes") Map<InstanceKey, Set<InstanceKey>> instanceRoutes,
                      @JsonProperty("properties") Map<String, String> properties) {
        this.serviceKey = serviceKey;
        this.enableAfterTime = enableAfterTime;
        this.disableAfterTime = disableAfterTime;
        this.instanceRoutes = instanceRoutes;
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "ChaosState{"
                + "serviceKey=" + serviceKey
                + ", enableAfterTime=" + enableAfterTime
                + ", disableAfterTime=" + disableAfterTime
                + ", instanceRoutes=" + instanceRoutes
                + ", properties=" + properties
                + '}';
    }

    @Override
    public int compareTo(ChaosState o) {
        return serviceKey.compareTo(o.serviceKey);
    }

}
