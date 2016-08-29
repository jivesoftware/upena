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
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LB implements Stored<LB>, Serializable {

    public final String name;
    public final String description;
    public final String scheme;
    public final int loadBalancerPort;
    public final int instancePort;
    public final List<String> availabilityZones;
    public final String protocol;
    public final String certificate;
    public final String serviceProtocol;

    public final List<String> securityGroups;
    public final List<String> subnets;
    public final Map<String, String> tags;

    public final ClusterKey clusterKey;
    public final ServiceKey serviceKey;
    public final ReleaseGroupKey releaseGroupKey;

    @JsonCreator
    public LB(@JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("scheme") String scheme,
        @JsonProperty("loadBalancerPort") int loadBalancerPort,
        @JsonProperty("instancePort") int instancePort,
        @JsonProperty("availabilityZones") List<String> availabilityZones,
        @JsonProperty("protocol") String protocol,
        @JsonProperty("certificate") String certificate,
        @JsonProperty("serviceProtocol") String serviceProtocol,
        @JsonProperty("securityGroups") List<String> securityGroups,
        @JsonProperty("subnets") List<String> subnets,
        @JsonProperty("tags") Map<String, String> tags,
        @JsonProperty("clusterKey") ClusterKey clusterKey,
        @JsonProperty("serviceKey") ServiceKey serviceKey,
        @JsonProperty("releaseGroupKey") ReleaseGroupKey releaseGroupKey) {

        this.name = name;
        this.description = description;
        this.scheme = scheme;
        this.loadBalancerPort = loadBalancerPort;
        this.instancePort = instancePort;
        this.availabilityZones = availabilityZones;
        this.protocol = protocol;
        this.certificate = certificate;
        this.serviceProtocol = serviceProtocol;
        this.securityGroups = securityGroups;
        this.subnets = subnets;
        this.tags = tags;
        this.clusterKey = clusterKey;
        this.serviceKey = serviceKey;
        this.releaseGroupKey = releaseGroupKey;
    }

    @Override
    public String toString() {
        return "LoadBalancer{"
            + "name=" + name
            + ", description=" + description
            + ", scheme=" + scheme
            + ", loadBalancerPort=" + loadBalancerPort
            + ", instancePort=" + instancePort
            + ", availabilityZones=" + availabilityZones
            + ", protocol=" + protocol
            + ", certificate=" + certificate
            + ", serviceProtocol=" + serviceProtocol
            + ", securityGroups=" + securityGroups
            + ", subnets=" + subnets
            + ", tags=" + tags
            + ", clusterKey=" + clusterKey
            + ", serviceKey=" + serviceKey
            + ", releaseGroupKey=" + releaseGroupKey
            + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LB other = (LB) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(LB o) {
        return name.compareTo(o.name);
    }

}
