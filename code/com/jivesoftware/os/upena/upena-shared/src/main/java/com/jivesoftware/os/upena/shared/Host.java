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

@JsonIgnoreProperties(ignoreUnknown = true)
public class Host implements Stored<Host>, Serializable {

    public final String name;
    public final String hostName;
    public final int port;
    public final String workingDirectory;
    public final ClusterKey clusterKey;

    @JsonCreator
    public Host(@JsonProperty("name") String name,
        @JsonProperty("hostName") String hostName,
        @JsonProperty("port") int port,
        @JsonProperty("workingDirectory") String workingDirectory,
        @JsonProperty("clusterKey") ClusterKey clusterKey) {
        this.name = name;
        this.hostName = hostName;
        this.port = port;
        this.workingDirectory = workingDirectory;
        this.clusterKey = clusterKey;
    }

    @Override
    public String toString() {
        return "Host{"
            + "name=" + name
            + ", hostName=" + hostName
            + ", port=" + port
            + ", workingDirectory=" + workingDirectory
            + ", clusterKey=" + clusterKey
            + '}';
    }

    @Override
    public int compareTo(Host o) {
        int i = hostName.compareTo(o.hostName);
        if (i == 0) {
            i = Integer.compare(port, o.port);
        }
        if (i == 0) {
            i = workingDirectory.compareTo(o.workingDirectory);
        }
        return i;
    }
}
