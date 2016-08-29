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
public class LoadBalancerFilter implements KeyValueFilter<LBKey, LB>, Serializable {

    public final String name;
    public final String clusterKey;
    public final String serviceKey;
    public final String releaseGroupKey;
    public final int start;
    public final int count;
    public int hit;

    @JsonCreator
    public LoadBalancerFilter(@JsonProperty("name") String name,
        @JsonProperty("clusterKey") String clusterKey,
        @JsonProperty("serviceKey") String serviceKey,
        @JsonProperty("releaseGroupKey") String releaseGroupKey,
        @JsonProperty("start") int start,
        @JsonProperty("count") int count) {
        this.name = name;
        this.clusterKey = clusterKey;
        this.serviceKey = serviceKey;
        this.releaseGroupKey = releaseGroupKey;
        this.start = start;
        this.count = count;
    }

    @Override
    public String toString() {
        return "LoadBalancerFilter{" + "name=" + name + ", clusterKey=" + clusterKey + ", serviceKey=" + serviceKey
            + ", releaseGroupKey=" + releaseGroupKey + ", start=" + start + ", count=" + count + ", hit=" + hit + '}';
    }

    

    @Override
    public ConcurrentNavigableMap<LBKey, TimestampedValue<LB>> createCollector() {
        return new Results();
    }

    public static class Results extends ConcurrentSkipListMap<LBKey, TimestampedValue<LB>> {
    }

    @Override
    public boolean filter(LBKey key, LB value) {
         if (name != null && !name.isEmpty() && value.name != null) {
            if (!value.name.contains(name)) {
                return false;
            }
        }
        if (clusterKey != null && !clusterKey.isEmpty() && value.clusterKey != null) {
            if (!value.clusterKey.equals(clusterKey)) {
                return false;
            }
        }
        if (serviceKey != null && !serviceKey.isEmpty() && value.serviceKey != null) {
            if (!value.serviceKey.equals(serviceKey)) {
                return false;
            }
        }
        if (releaseGroupKey != null && !releaseGroupKey.isEmpty()  && value.releaseGroupKey != null) {
            if (!value.releaseGroupKey.equals(releaseGroupKey)) {
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
