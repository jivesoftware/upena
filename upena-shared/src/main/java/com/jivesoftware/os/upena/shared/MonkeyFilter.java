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
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MonkeyFilter implements KeyValueFilter<MonkeyKey, Monkey>, Serializable {

    public final ClusterKey clusterKey;
    public final HostKey hostKey;
    public final ServiceKey serviceKey;
    public final ChaosStrategyKey strategyKey;
    public final int start;
    public final int count;
    public int hit;

    @JsonCreator
    public MonkeyFilter(@JsonProperty("clusterKey") ClusterKey clusterKey,
                        @JsonProperty("hostKey") HostKey hostKey,
                        @JsonProperty("serviceKey") ServiceKey serviceKey,
                        @JsonProperty("strategyKey") ChaosStrategyKey strategyKey,
                        @JsonProperty("start") int start,
                        @JsonProperty("count") int count) {
        this.clusterKey = clusterKey;
        this.hostKey = hostKey;
        this.serviceKey = serviceKey;
        this.strategyKey = strategyKey;
        this.start = start;
        this.count = count;
    }

    @Override
    public String toString() {
        return "InstanceFilter{"
                + "clusterKey=" + clusterKey
                + ", hostKey=" + hostKey
                + ", serviceKey=" + serviceKey
                + ", strategyKey=" + strategyKey
                + ", start=" + start
                + ", count=" + count
                + ", hit=" + hit
                + '}';
    }

    @Override
    public ConcurrentNavigableMap<MonkeyKey, TimestampedValue<Monkey>> createCollector() {
        return new Results();
    }

    public static class Results extends ConcurrentSkipListMap<MonkeyKey, TimestampedValue<Monkey>> {
    }

    @Override
    public boolean filter(MonkeyKey key, Monkey value) {
        if (clusterKey != null && value.clusterKey != null) {
            if (!value.clusterKey.equals(clusterKey)) {
                return false;
            }
        }
        if (hostKey != null && value.hostKey != null) {
            if (!value.hostKey.equals(hostKey)) {
                return false;
            }
        }
        if (serviceKey != null && value.serviceKey != null) {
            if (!value.serviceKey.equals(serviceKey)) {
                return false;
            }
        }
        if (strategyKey != null && value.strategyKey != null) {
            if (!value.strategyKey.equals(strategyKey)) {
                return false;
            }
        }

        hit++;
        if (hit < start) {
            return false;
        }
        if (hit > start + count) {
            return false;
        }
        return true;
    }

    @Override
    public void reset() {
        hit = 0;
    }
}
