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
public class HostFilter implements KeyValueFilter<HostKey, Host>, Serializable {

    public final String name;
    public final String hostName;
    public final String workingDirectory;
    public final Integer port;
    public final ClusterKey clusterKey;
    public final int start;
    public final int count;
    public int hit;

    @JsonCreator
    public HostFilter(@JsonProperty("name") String name,
        @JsonProperty("hostName") String hostName,
        @JsonProperty("port") Integer port,
        @JsonProperty("workingDirectory") String workingDirectory,
        @JsonProperty("clusterKey") ClusterKey clusterKey,
        @JsonProperty("start") int start,
        @JsonProperty("count") int count) {
        this.name = name;
        this.hostName = hostName;
        this.port = port;
        this.workingDirectory = workingDirectory;
        this.clusterKey = clusterKey;
        this.start = start;
        this.count = count;
    }

    @Override
    public String toString() {
        return "HostFilter{"
            + "name=" + name
            + ", hostName=" + hostName
            + ", port=" + port
            + ", workingDirectory=" + workingDirectory
            + ", clusterKey=" + clusterKey
            + ", start=" + start
            + ", count=" + count
            + ", hit=" + hit
            + '}';
    }

    @Override
    public ConcurrentNavigableMap<HostKey, TimestampedValue<Host>> createCollector() {
        return new Results();
    }

    public static class Results extends ConcurrentSkipListMap<HostKey, TimestampedValue<Host>> {
    }

    @Override
    public boolean filter(HostKey key, Host value) {
        if (name != null && value.name != null) {
            if (!value.name.contains(name)) {
                return false;
            }
        }
        if (hostName != null && value.hostName != null) {
            if (!value.hostName.contains(hostName)) {
                return false;
            }
        }
        if (workingDirectory != null && value.workingDirectory != null) {
            if (!value.workingDirectory.contains(workingDirectory)) {
                return false;
            }
        }
        if (clusterKey != null && value.clusterKey != null) {
            if (!value.clusterKey.equals(clusterKey)) {
                return false;
            }
        }
        if (port != null) {
            if (port != value.port) {
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
