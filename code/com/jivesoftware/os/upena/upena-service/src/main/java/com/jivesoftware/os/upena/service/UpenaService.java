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
package com.jivesoftware.os.upena.service;

import com.jivesoftware.os.amza.shared.TimestampedValue;
import com.jivesoftware.os.upena.routing.shared.ConnectionDescriptor;
import com.jivesoftware.os.upena.routing.shared.ConnectionDescriptorsRequest;
import com.jivesoftware.os.upena.routing.shared.ConnectionDescriptorsResponse;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptor;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptorsRequest;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptorsResponse;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.Instance.Port;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceFilter;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.Tenant;
import com.jivesoftware.os.upena.shared.TenantKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;

public class UpenaService {

    private final UpenaStore upenaStore;

    public UpenaService(UpenaStore upenaStore) throws Exception {
        this.upenaStore = upenaStore;
    }

    public ConnectionDescriptorsResponse connectionRequest(ConnectionDescriptorsRequest connectionsRequest) throws Exception {

        InstanceKey instanceKey = new InstanceKey(connectionsRequest.getInstanceId());
        Instance instance = upenaStore.instances.get(instanceKey);
        ConnectionDescriptorsResponse invalidIfNotNull = ensureInstanceIsValid(instanceKey, instance);
        if (invalidIfNotNull != null) {
            return invalidIfNotNull;
        }

        Tenant tenant = null;
        if (connectionsRequest.getTenantId() != null && !connectionsRequest.getTenantId().equals("*") && connectionsRequest.getTenantId().length() > 0) {
            TenantKey tenantkey = new TenantKey(connectionsRequest.getTenantId());
            tenant = upenaStore.tenants.get(tenantkey);
            if (tenant == null) {
                return failedConnectionResponse("Undeclared tenant for tenantkey:" + tenantkey);
            }
        }

        String connectToServiceNamed = connectionsRequest.getConnectToServiceNamed();
        ServiceFilter serviceFilter = new ServiceFilter(connectToServiceNamed, null, 0, Integer.MAX_VALUE);
        ConcurrentNavigableMap<ServiceKey, TimestampedValue<Service>> gotServices = upenaStore.services.find(serviceFilter);
        if (gotServices.isEmpty()) {
            return failedConnectionResponse("Undeclared service connectToServiceNamed:" + connectToServiceNamed);
        }
        if (gotServices.size() > 1) {
            return failedConnectionResponse("More that one service decalred for connectToServiceNamed:" + connectToServiceNamed);
        }
        ServiceKey wantToConnectToServiceKey = new ServiceKey(gotServices.firstKey().getKey());

        ReleaseGroupKey ownerReleaseGroupKey = null;
        ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = null;
        if (tenant != null
                && tenant.releaseGroupKey != null
                && tenant.releaseGroupKey.getKey() != null
                && tenant.releaseGroupKey.getKey().length() > 0) {
            ownerReleaseGroupKey = tenant.releaseGroupKey; // Use the release group assigned to the tenant.
            InstanceFilter explicityReleaseGroupFilter = new InstanceFilter(instance.clusterKey,
                    null, wantToConnectToServiceKey, ownerReleaseGroupKey, null, 0, Integer.MAX_VALUE);
            got = upenaStore.instances.find(explicityReleaseGroupFilter);
        }

        if (got == null || got.isEmpty()) {
            Cluster cluster = upenaStore.clusters.get(instance.clusterKey);
            if (cluster == null) {
                // garbage instance. should be removed?
                return failedConnectionResponse("Undeclared cluster for clusterKey:" + cluster);
            }
            ownerReleaseGroupKey = cluster.defaultReleaseGroups.get(wantToConnectToServiceKey); // Use instance assigned to the instances cluster.
            if (ownerReleaseGroupKey == null) {
                return failedConnectionResponse("Cluster:" + cluster + " doesen't have a release group declared for "
                        + "serviceKey:" + wantToConnectToServiceKey + " .");
            }
            ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(ownerReleaseGroupKey);
            if (releaseGroup == null) {
                return failedConnectionResponse("Cluster:" + cluster + " releaseGroup:" + ownerReleaseGroupKey + " is undeclared.");
            }
            InstanceFilter explicityReleaseGroupFilter = new InstanceFilter(instance.clusterKey,
                    null, wantToConnectToServiceKey, ownerReleaseGroupKey, null, 0, Integer.MAX_VALUE);
            got = upenaStore.instances.find(explicityReleaseGroupFilter);
        }

        if (got == null || got.isEmpty()) {
            return failedConnectionResponse("No declared instance for: " + connectionsRequest);
        }

        List<String> messages = new ArrayList<>();
        List<ConnectionDescriptor> connections = new ArrayList<>();
        for (Entry<InstanceKey, TimestampedValue<Instance>> entry : got.entrySet()) {
            Instance value = entry.getValue().getValue();

            Host host = upenaStore.hosts.get(value.hostKey);
            if (host == null) {
                // garbage instance. should be removed?
            } else {
                Port port = value.ports.get(connectionsRequest.getPortName());
                if (port == null) {
                    messages.add("instanceKey:" + entry.getKey() + " doesn't have a port declared for '" + connectionsRequest.getPortName() + "'");
                } else {
                    Map<String, String> properties = new HashMap<>();
                    properties.putAll(port.properties);
                    connections.add(new ConnectionDescriptor(host.hostName, port.port, properties));
                }
            }
        }

        Set<String> validReleaseGroupKeys = getValidReleaseGroupKeys(instance.clusterKey, wantToConnectToServiceKey);

        messages.add("Success");
        return new ConnectionDescriptorsResponse(1, messages, ownerReleaseGroupKey.getKey(), connections, validReleaseGroupKeys);
    }

    ConnectionDescriptorsResponse ensureInstanceIsValid(InstanceKey instanceKey, Instance instance) throws Exception {
        if (instance == null) {
            return failedConnectionResponse("Undeclared instance for instanceKey:" + instanceKey);
        }

        if (!instance.enabled) {
            return failedConnectionResponse("Instance has been disabled.");
        }

        Cluster cluster = upenaStore.clusters.get(instance.clusterKey);
        if (cluster == null) {
            return failedConnectionResponse("Instance has been decommisioned clusterKey:" + instance.clusterKey + " no longer exists.");
        }

        Host host = upenaStore.hosts.get(instance.hostKey);
        if (host == null) {
            return failedConnectionResponse("Instance has been decommisioned hostKey:" + instance.hostKey + " no longer exists.");
        }

        ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(instance.releaseGroupKey);
        if (releaseGroup == null) {
            return failedConnectionResponse("Instance has been decommisioned releaseGroupKey:" + instance.releaseGroupKey + " no longer exists.");
        }
        return null;
    }

    private ConnectionDescriptorsResponse failedConnectionResponse(String... message) {
        return new ConnectionDescriptorsResponse(-1,
                Arrays.asList(message), null, null, null);
    }

    Set<String> getValidReleaseGroupKeys(ClusterKey clusterKey, ServiceKey serviceKey) throws Exception {
        Set<String> validReleaseGroupIds = new HashSet<>();
        InstanceFilter filter = new InstanceFilter(clusterKey, null, serviceKey, null, null, 0, Integer.MAX_VALUE);
        ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = upenaStore.instances.find(filter);
        for (Entry<InstanceKey, TimestampedValue<Instance>> entry : got.entrySet()) {
            Instance value = entry.getValue().getValue();
            validReleaseGroupIds.add(value.releaseGroupKey.getKey());
        }
        return validReleaseGroupIds;
    }

    public InstanceDescriptorsResponse instanceDescriptors(InstanceDescriptorsRequest instanceDescriptorsRequest) throws Exception {
        HostKey hostKey = new HostKey(instanceDescriptorsRequest.hostKey);
        Host host = upenaStore.hosts.get(hostKey);
        if (host == null) {
            return new InstanceDescriptorsResponse(instanceDescriptorsRequest.hostKey, true);
        }

        InstanceDescriptorsResponse instanceDescriptorsResponse = new InstanceDescriptorsResponse(instanceDescriptorsRequest.hostKey, false);
        InstanceFilter impactedFilter = new InstanceFilter(null, hostKey, null, null, null, 0, Integer.MAX_VALUE);
        ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = upenaStore.instances.find(impactedFilter);
        for (Entry<InstanceKey, TimestampedValue<Instance>> e : got.entrySet()) {
            Instance instance = e.getValue().getValue();
            if (!instance.enabled) {
                continue;
            }
            ClusterKey clusterKey = instance.clusterKey;
            Cluster cluster = upenaStore.clusters.get(clusterKey);
            if (cluster == null) {
                upenaStore.instances.remove(e.getKey());
                continue;
            }
            String clusterName = cluster.name;

            ServiceKey serviceKey = instance.serviceKey;
            Service service = upenaStore.services.get(serviceKey);
            if (service == null) {
                upenaStore.instances.remove(e.getKey());
                continue;
            }
            String serviceName = service.name;

            ReleaseGroupKey releaseGroupKey = instance.releaseGroupKey;
            ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(releaseGroupKey);
            if (releaseGroup == null) {
                upenaStore.instances.remove(e.getKey());
                continue;
            }
            String releaseGroupName = releaseGroup.email;
            InstanceKey instanceKey = e.getKey();

            InstanceDescriptor instanceDescriptor = new InstanceDescriptor(clusterKey.getKey(),
                    clusterName,
                    serviceKey.getKey(),
                    serviceName,
                    releaseGroupKey.getKey(),
                    releaseGroupName,
                    instanceKey.getKey(),
                    instance.instanceId,
                    releaseGroup.version);

            for (Entry<String, Instance.Port> p : instance.ports.entrySet()) {
                instanceDescriptor.ports.put(p.getKey(), new InstanceDescriptor.InstanceDescriptorPort(p.getValue().port));
            }

            instanceDescriptorsResponse.instanceDescriptors.add(instanceDescriptor);
        }

        return instanceDescriptorsResponse;
    }
}