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

import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptorsRequest;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptorsResponse;
import com.jivesoftware.os.routing.bird.shared.HostPort;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptor;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptorsRequest;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptorsResponse;
import com.jivesoftware.os.uba.shared.PasswordStore;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.Instance.Port;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroup.Type;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.Tenant;
import com.jivesoftware.os.upena.shared.TenantKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentNavigableMap;

public class UpenaService {

    private final PasswordStore passwordStore;
    private final SessionStore sessionStore;

    private final UpenaStore upenaStore;
    private final ChaosService chaosService;
    private final InstanceHealthly instanceHealthly;

    public UpenaService(PasswordStore passwordStore, SessionStore sessionStore, UpenaStore upenaStore, ChaosService chaosService,
        InstanceHealthly instanceHealthly) throws Exception {
        this.passwordStore = passwordStore;
        this.sessionStore = sessionStore;
        this.upenaStore = upenaStore;
        this.chaosService = chaosService;
        this.instanceHealthly = instanceHealthly;
    }

    public ConnectionDescriptorsResponse connectionRequest(ConnectionDescriptorsRequest connectionsRequest) throws Exception {
        InstanceKey instanceKey = new InstanceKey(connectionsRequest.getInstanceId());
        Instance instance = upenaStore.instances.get(instanceKey);
        if (instance == null) {
            return failedConnectionResponse(connectionsRequest, "Undeclared instance for instanceKey:" + instanceKey);
        }

        if (!instance.enabled) {
            return failedConnectionResponse(connectionsRequest, "Instance has been disabled.");
        }

        Cluster cluster = upenaStore.clusters.get(instance.clusterKey);
        if (cluster == null) {
            return failedConnectionResponse(connectionsRequest, "Instance has been decommissioned clusterKey:" + instance.clusterKey + " no longer exists.");
        }

        Host host = upenaStore.hosts.get(instance.hostKey);
        if (host == null) {
            return failedConnectionResponse(connectionsRequest, "Instance has been decommissioned hostKey:" + instance.hostKey + " no longer exists.");
        }

        ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(instance.releaseGroupKey);
        if (releaseGroup == null) {
            return failedConnectionResponse(connectionsRequest,
                "Instance has been decommissioned releaseGroupKey:" + instance.releaseGroupKey + " no longer exists.");
        }

        List<String> messages = new ArrayList<>();
        Tenant tenant = null;
        if (connectionsRequest.getTenantId() != null && !connectionsRequest.getTenantId().equals("*") && connectionsRequest.getTenantId().length() > 0) {
            TenantKey tenantKey = new TenantKey(connectionsRequest.getTenantId());
            tenant = upenaStore.tenants.get(tenantKey);
            if (tenant == null) {
                messages.add("no tenant defined for tenantKey:" + tenantKey);
            }
        }

        ServiceKey[] serviceKey = new ServiceKey[1];
        upenaStore.services.scan((key, value) -> {
            if (value != null && value.name.equals(connectionsRequest.getConnectToServiceNamed())) {
                serviceKey[0] = key;
                return false;
            }
            return true;
        });

        Service service = serviceKey[0] == null ? null : upenaStore.services.get(serviceKey[0]);
        if (service == null) {
            return failedConnectionResponse(connectionsRequest,
                "Undeclared service connectToServiceNamed:" + connectionsRequest.getConnectToServiceNamed());
        }

        ServiceKey wantToConnectToServiceKey = serviceKey[0];

        ReleaseGroupKey releaseGroupKey = null;
        List<ConnectionDescriptor> primaryConnections = null;

        if (tenant != null) {
            releaseGroupKey = tenant.overrideReleaseGroups.get(wantToConnectToServiceKey);
            if (releaseGroupKey != null) {
                ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = findInstances(messages,
                    instance.clusterKey, releaseGroupKey, wantToConnectToServiceKey);
                if (got != null && !got.isEmpty()) {
                    primaryConnections = buildConnections(messages, got, connectionsRequest.getPortName());
                }
            }
        }
        if (primaryConnections == null) {
            releaseGroupKey = cluster.defaultReleaseGroups.get(wantToConnectToServiceKey); // Use instance assigned to the instances cluster.
            if (releaseGroupKey == null) {
                return failedConnectionResponse(connectionsRequest,
                    "Cluster:" + cluster + " does not have a release group declared for serviceKey:" + wantToConnectToServiceKey);
            }
            ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = findInstances(messages,
                instance.clusterKey, releaseGroupKey, wantToConnectToServiceKey);
            if (got != null && !got.isEmpty()) {
                primaryConnections = buildConnections(messages, got, connectionsRequest.getPortName());
            }
        }

        if (primaryConnections == null || primaryConnections.isEmpty()) {
            return failedConnectionResponse(connectionsRequest, "No declared instance for: " + connectionsRequest);
        }

        // handle instance and service monkeys
        for (HostKey hk : Arrays.asList(instance.hostKey, new HostKey(""))) {
            String monkeyAffectInstances = chaosService.monkeyAffect(instance.clusterKey, hk, instance.serviceKey);
            if (!monkeyAffectInstances.isEmpty()) {
                primaryConnections = chaosService.unleashMonkey(
                    instanceKey,
                    instance,
                    hk,
                    primaryConnections);
                messages.add("Monkey affect: [" + monkeyAffectInstances + "]");
            }
        }

        messages.add("Success");
        return new ConnectionDescriptorsResponse(1,
            messages,
            (releaseGroupKey == null) ? null : releaseGroupKey.getKey(),
            primaryConnections,
            connectionsRequest.getRequestUuid());
    }

    private ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> findInstances(List<String> messages,
        ClusterKey clusterKey,
        ReleaseGroupKey releaseGroupKey,
        ServiceKey wantToConnectToServiceKey) throws Exception {
        if (releaseGroupKey == null) {
            messages.add("Provided null releaseGroupKey so give null back.");
            return null;
        }

        ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(releaseGroupKey);
        if (releaseGroup == null) {
            messages.add("Warning releaseGroup:" + releaseGroupKey + " is undeclared.");
            return null;
        }

        if (releaseGroupKey.getKey() != null
            && releaseGroupKey.getKey().length() > 0) {
            InstanceFilter explicitReleaseGroupFilter = new InstanceFilter(clusterKey,
                null, wantToConnectToServiceKey, releaseGroupKey, null, 0, Integer.MAX_VALUE);
            return upenaStore.instances.find(false, explicitReleaseGroupFilter);
        }
        return null;
    }

    private List<ConnectionDescriptor> buildConnections(List<String> messages,
        ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> instances,
        String portName) throws Exception {

        List<ConnectionDescriptor> connections = new ArrayList<>();

        for (Entry<InstanceKey, TimestampedValue<Instance>> entry : instances.entrySet()) {
            InstanceKey key = entry.getKey();
            Instance value = entry.getValue().getValue();
            if (value.enabled) {
                InstanceDescriptor instanceDescriptor = toInstanceDescriptor(key, value);
                if (instanceDescriptor != null) {
                    Host host = upenaStore.hosts.get(value.hostKey);
                    if (host == null) {
                        // garbage instance. should be removed?
                    } else {
                        Port port = value.ports.get(portName);
                        if (port == null) {
                            messages.add("instanceKey:" + key + " does not have a port declared for '" + portName + "'");
                        } else {
                            Map<String, String> properties = new HashMap<>();
                            properties.putAll(port.properties);
                            connections.add(new ConnectionDescriptor(
                                instanceDescriptor,
                                port.sslEnabled,
                                port.serviceAuthEnabled,
                                new HostPort(host.hostName, port.port),
                                properties,
                                null)
                            );
                        }
                    }
                }
            } else {
                messages.add("instanceKey:" + key + " is not enabled");
            }
        }

        return connections;
    }

    private ConnectionDescriptorsResponse failedConnectionResponse(ConnectionDescriptorsRequest request, String... message) {
        return new ConnectionDescriptorsResponse(-1, Arrays.asList(message), null, null, request.getRequestUuid());
    }

    public InstanceDescriptorsResponse instanceDescriptors(InstanceDescriptorsRequest instanceDescriptorsRequest) throws Exception {
        HostKey hostKey = new HostKey(instanceDescriptorsRequest.hostKey);
        Host host = upenaStore.hosts.get(hostKey);
        if (host == null) {
            return new InstanceDescriptorsResponse(instanceDescriptorsRequest.hostKey, true);
        }

        InstanceDescriptorsResponse instanceDescriptorsResponse = new InstanceDescriptorsResponse(instanceDescriptorsRequest.hostKey, false);
        InstanceFilter impactedFilter = new InstanceFilter(null, hostKey, null, null, null, 0, Integer.MAX_VALUE);
        ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = upenaStore.instances.find(false, impactedFilter);
        for (Entry<InstanceKey, TimestampedValue<Instance>> e : got.entrySet()) {
            InstanceDescriptor instanceDescriptor = toInstanceDescriptor(e.getKey(), e.getValue().getValue());
            if (instanceDescriptor != null) {
                instanceDescriptorsResponse.instanceDescriptors.add(instanceDescriptor);
            }
        }
        return instanceDescriptorsResponse;
    }

    private InstanceDescriptor toInstanceDescriptor(InstanceKey instanceKey, Instance instance) throws Exception {
        ClusterKey clusterKey = instance.clusterKey;

        Host host = upenaStore.hosts.get(instance.hostKey);
        if (host == null) {
            return null;
        }

        Cluster cluster = upenaStore.clusters.get(clusterKey);
        if (cluster == null) {
            return null;
        }
        String clusterName = cluster.name;

        ServiceKey serviceKey = instance.serviceKey;
        Service service = upenaStore.services.get(serviceKey);
        if (service == null) {
            return null;
        }
        String serviceName = service.name;

        ReleaseGroupKey releaseGroupKey = instance.releaseGroupKey;
        ReleaseGroup releaseGroup = upenaStore.releaseGroups.get(releaseGroupKey);
        if (releaseGroup == null) {
            return null;
        }
        String releaseGroupName = releaseGroup.name;

        InstanceDescriptor instanceDescriptor;
        if (releaseGroup.type == Type.stable || releaseGroup.rollbackVersion == null) {
            instanceDescriptor = new InstanceDescriptor(host.datacenterName,
                host.rackName,
                host.name,
                clusterKey.getKey(),
                clusterName,
                serviceKey.getKey(),
                serviceName,
                releaseGroupKey.getKey(),
                releaseGroupName,
                instanceKey.getKey(),
                instance.instanceId,
                releaseGroup.version,
                releaseGroup.repository,
                instance.publicKey,
                instance.restartTimestampGMTMillis,
                instance.enabled);
        } else if (releaseGroup.type == Type.immediate) {

            instanceDescriptor = new InstanceDescriptor(host.datacenterName,
                host.rackName,
                host.name,
                clusterKey.getKey(),
                clusterName,
                serviceKey.getKey(),
                serviceName,
                releaseGroupKey.getKey(),
                releaseGroupName,
                instanceKey.getKey(),
                instance.instanceId,
                releaseGroup.version,
                releaseGroup.repository,
                instance.publicKey,
                instance.restartTimestampGMTMillis,
                instance.enabled);

            InstanceFilter filter = new InstanceFilter(clusterKey, null, serviceKey, releaseGroupKey, null, 0, Integer.MAX_VALUE);
            ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> instances = upenaStore.instances.find(false, filter);

            int count = instances.size();
            for (Entry<InstanceKey, TimestampedValue<Instance>> e : instances.entrySet()) {
                if (!e.getValue().getTombstoned()) {
                    if (instanceHealthly.isHealth(e.getKey(), releaseGroup.version)) {
                        count--;
                    }
                } else {
                    count--;
                }
            }

            if (count == 0 && releaseGroup.type != Type.stable) {
                ReleaseGroup newReleaseGroup = new ReleaseGroup(Type.stable,
                    releaseGroup.name,
                    releaseGroup.email,
                    releaseGroup.rollbackVersion,
                    releaseGroup.version,
                    releaseGroup.repository,
                    releaseGroup.description,
                    releaseGroup.autoRelease,
                    releaseGroup.properties
                );
                upenaStore.releaseGroups.update(releaseGroupKey, newReleaseGroup);
            }

        } else if (releaseGroup.type == Type.canary) {
            InstanceFilter filter = new InstanceFilter(clusterKey, null, serviceKey, releaseGroupKey, null, 0, Integer.MAX_VALUE);
            ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> instances = upenaStore.instances.find(false, filter);
            int minInstanceId = Integer.MAX_VALUE;
            for (TimestampedValue<Instance> instanceTimestampedValue : instances.values()) {
                if (!instanceTimestampedValue.getTombstoned()) {
                    minInstanceId = Math.min(minInstanceId, instanceTimestampedValue.getValue().instanceId);
                }
            }
            if (instance.instanceId == minInstanceId) {
                instanceDescriptor = new InstanceDescriptor(host.datacenterName,
                    host.rackName,
                    host.name,
                    clusterKey.getKey(),
                    clusterName,
                    serviceKey.getKey(),
                    serviceName,
                    releaseGroupKey.getKey(),
                    releaseGroupName,
                    instanceKey.getKey(),
                    instance.instanceId,
                    releaseGroup.version,
                    releaseGroup.repository,
                    instance.publicKey,
                    instance.restartTimestampGMTMillis,
                    instance.enabled);
            } else {
                instanceDescriptor = new InstanceDescriptor(host.datacenterName,
                    host.rackName,
                    host.name,
                    clusterKey.getKey(),
                    clusterName,
                    serviceKey.getKey(),
                    serviceName,
                    releaseGroupKey.getKey(),
                    releaseGroupName,
                    instanceKey.getKey(),
                    instance.instanceId,
                    releaseGroup.rollbackVersion,
                    releaseGroup.repository,
                    instance.publicKey,
                    instance.restartTimestampGMTMillis,
                    instance.enabled);
            }

        } else if (releaseGroup.type == Type.rolling) {


            InstanceFilter filter = new InstanceFilter(clusterKey, null, serviceKey, releaseGroupKey, null, 0, Integer.MAX_VALUE);
            ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> instances = upenaStore.instances.find(false, filter);

            boolean anybodyLessThanMeUnhealthy = false;
            boolean amIUnhealthy = false;
            boolean anybodyUnhealthy = false;
            Map<InstanceKey, Boolean> healths = new HashMap<>();

            for (Entry<InstanceKey, TimestampedValue<Instance>> e : instances.entrySet()) {
                if (!e.getValue().getTombstoned()) {
                    boolean healthy = instanceHealthly.isHealth(e.getKey(), releaseGroup.version);
                    healths.put(e.getKey(), healthy);
                    int instanceId = e.getValue().getValue().instanceId;
                    if (instanceId < instance.instanceId) {
                        anybodyLessThanMeUnhealthy |= !healthy;
                    } else if (instanceId == instance.instanceId) {
                        amIUnhealthy = !healthy;
                    }
                    anybodyUnhealthy |= !healthy;
                }
            }

            if (amIUnhealthy && !anybodyLessThanMeUnhealthy) {
                instanceDescriptor = new InstanceDescriptor(host.datacenterName,
                    host.rackName,
                    host.name,
                    clusterKey.getKey(),
                    clusterName,
                    serviceKey.getKey(),
                    serviceName,
                    releaseGroupKey.getKey(),
                    releaseGroupName,
                    instanceKey.getKey(),
                    instance.instanceId,
                    releaseGroup.version,
                    releaseGroup.repository,
                    instance.publicKey,
                    instance.restartTimestampGMTMillis,
                    instance.enabled);
            } else {
                instanceDescriptor = new InstanceDescriptor(host.datacenterName,
                    host.rackName,
                    host.name,
                    clusterKey.getKey(),
                    clusterName,
                    serviceKey.getKey(),
                    serviceName,
                    releaseGroupKey.getKey(),
                    releaseGroupName,
                    instanceKey.getKey(),
                    instance.instanceId,
                    healths.get(instanceKey) ? releaseGroup.version : releaseGroup.rollbackVersion,
                    releaseGroup.repository,
                    instance.publicKey,
                    instance.restartTimestampGMTMillis,
                    instance.enabled);
            }


            if (!anybodyUnhealthy && releaseGroup.type != Type.stable) {
                ReleaseGroup newReleaseGroup = new ReleaseGroup(Type.stable,
                    releaseGroup.name,
                    releaseGroup.email,
                    releaseGroup.rollbackVersion,
                    releaseGroup.version,
                    releaseGroup.repository,
                    releaseGroup.description,
                    releaseGroup.autoRelease,
                    releaseGroup.properties
                );
                upenaStore.releaseGroups.update(releaseGroupKey, newReleaseGroup);
            }
        } else {
            throw new IllegalStateException("Unexpected state:" + releaseGroup.type);
        }

        for (Entry<String, Instance.Port> p : instance.ports.entrySet()) {
            Port value = p.getValue();
            instanceDescriptor.ports.put(p.getKey(), new InstanceDescriptor.InstanceDescriptorPort(value.sslEnabled, value.serviceAuthEnabled, value.port));
        }

        return instanceDescriptor;
    }

    public String instancePublicKey(String instanceKey) throws Exception {
        Instance instance = upenaStore.instances.get(new InstanceKey(instanceKey));
        if (instance != null) {
            return instance.publicKey;
        }
        return null;
    }

    public String keyStorePassword(String instanceKey) throws Exception {
        return passwordStore.password(instanceKey);
    }

    public boolean isValid(SessionValidation sessionValidation) {
        return sessionStore.isValid(sessionValidation);
    }

}
