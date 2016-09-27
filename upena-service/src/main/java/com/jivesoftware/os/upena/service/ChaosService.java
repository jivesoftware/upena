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

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.HostPort;
import com.jivesoftware.os.upena.shared.ChaosState;
import com.jivesoftware.os.upena.shared.ChaosStateFilter;
import com.jivesoftware.os.upena.shared.ChaosStateKey;
import com.jivesoftware.os.upena.shared.ChaosStrategyKey;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.Monkey;
import com.jivesoftware.os.upena.shared.MonkeyFilter;
import com.jivesoftware.os.upena.shared.MonkeyKey;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChaosService {

    private final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final UpenaStore upenaStore;

    private static final String IPADDRESS_PATTERN =
            "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$";
    private static final Pattern ipPattern = Pattern.compile(IPADDRESS_PATTERN);

    public ChaosService(UpenaStore upenaStore) {
        this.upenaStore = upenaStore;
    }

    String monkeyAffect(ClusterKey clusterKey,
                        HostKey hostKey,
                        ServiceKey serviceKey) throws Exception {
        String affect = "";

        ConcurrentNavigableMap<MonkeyKey, TimestampedValue<Monkey>> gotMonkeys =
                upenaStore.monkeys.find(new MonkeyFilter(
                        clusterKey, hostKey, serviceKey,
                        null, 0, Integer.MAX_VALUE));

        String prefix = "'";
        String postfix = "'";
        for (Entry<MonkeyKey, TimestampedValue<Monkey>> entry : gotMonkeys.entrySet()) {
            Monkey value = entry.getValue().getValue();

            if (value.enabled) {
                if (!affect.isEmpty()) {
                    prefix = ", '";
                }
                affect += prefix + value.strategyKey + postfix;
            }
        }

        return affect;
    }

    List<ConnectionDescriptor> unleashMonkey(InstanceKey instanceKey,
                                             Instance instance,
                                             HostKey hostKey,
                                             List<ConnectionDescriptor> connections) throws Exception {
        List<ConnectionDescriptor> monkeyConnections = connections;

        ConcurrentNavigableMap<MonkeyKey, TimestampedValue<Monkey>> gotMonkeys =
                upenaStore.monkeys.find(new MonkeyFilter(
                        instance.clusterKey, hostKey, instance.serviceKey,
                        null, 0, Integer.MAX_VALUE));

        for (Entry<MonkeyKey, TimestampedValue<Monkey>> entry : gotMonkeys.entrySet()) {
            Monkey value = entry.getValue().getValue();

            if (value.enabled) {
                {
                    List<ConnectionDescriptor> connectionsTemp = new ArrayList<>();

                    for (ConnectionDescriptor connectionDescriptor : monkeyConnections) {
                        Map<String, String> newMonkeys =
                                (connectionDescriptor.getMonkeys() != null) ? connectionDescriptor.getMonkeys() : new HashMap<>();
                        newMonkeys.put(value.strategyKey.toString(),
                                value.strategyKey.description);

                        ConnectionDescriptor newConnectionDescriptor = new ConnectionDescriptor(
                                connectionDescriptor.getInstanceDescriptor(),
                                connectionDescriptor.getHostPort(),
                                connectionDescriptor.getProperties(),
                                newMonkeys);
                        connectionsTemp.add(newConnectionDescriptor);
                    }

                    monkeyConnections = connectionsTemp;
                }

                if (value.strategyKey == ChaosStrategyKey.RANDOMIZE_HOSTNAME) {
                    List<ConnectionDescriptor> connectionsTemp = new ArrayList<>();

                    for (ConnectionDescriptor connectionDescriptor : monkeyConnections) {
                        HostPort newHostPort = new HostPort(
                                randomizeHost(connectionDescriptor.getHostPort().getHost()),
                                connectionDescriptor.getHostPort().getPort());

                        ConnectionDescriptor newConnectionDescriptor = new ConnectionDescriptor(
                                connectionDescriptor.getInstanceDescriptor(),
                                newHostPort,
                                connectionDescriptor.getProperties(),
                                connectionDescriptor.getMonkeys());
                        connectionsTemp.add(newConnectionDescriptor);
                    }

                    monkeyConnections = connectionsTemp;
                } else if (value.strategyKey == ChaosStrategyKey.RANDOMIZE_PORT) {
                    List<ConnectionDescriptor> connectionsTemp = new ArrayList<>();

                    for (ConnectionDescriptor connectionDescriptor : monkeyConnections) {
                        HostPort newHostPort = new HostPort(
                                connectionDescriptor.getHostPort().getHost(),
                                randomizePort(connectionDescriptor.getHostPort().getPort()));

                        ConnectionDescriptor newConnectionDescriptor = new ConnectionDescriptor(
                                connectionDescriptor.getInstanceDescriptor(),
                                newHostPort,
                                connectionDescriptor.getProperties(),
                                connectionDescriptor.getMonkeys());
                        connectionsTemp.add(newConnectionDescriptor);
                    }

                    monkeyConnections = connectionsTemp;
                } else if (value.strategyKey == ChaosStrategyKey.SPLIT_BRAIN) {
                    //
                    // sort connections by instance name+key and
                    // return list of connections from whichever half the instance lives
                    // with the second half being the bigger half
                    //

                    monkeyConnections.sort((ConnectionDescriptor o1, ConnectionDescriptor o2) -> {
                        int c = Integer.compare(o1.getInstanceDescriptor().instanceName, o2.getInstanceDescriptor().instanceName);
                        if (c != 0) {
                            return c;
                        }
                        return o1.getInstanceDescriptor().instanceKey.compareTo(o2.getInstanceDescriptor().instanceKey);
                    });

                    ConnectionDescriptor[] monkeyConnectionsArray =
                            monkeyConnections.toArray(new ConnectionDescriptor[monkeyConnections.size()]);

                    int iterBegin = 0;
                    int iterEnd = monkeyConnectionsArray.length / 2;
                    for (int i = iterEnd; i < monkeyConnectionsArray.length; i++) {
                        if (monkeyConnectionsArray[i].getInstanceDescriptor().instanceName == instance.instanceId) {
                            iterBegin = iterEnd;
                            iterEnd = monkeyConnectionsArray.length;
                            break;
                        }
                    }

                    List<ConnectionDescriptor> connectionsTemp = new ArrayList<>();

                    for (int i = iterBegin; i < iterEnd; i++) {
                        ConnectionDescriptor newConnectionDescriptor = new ConnectionDescriptor(
                                monkeyConnectionsArray[i].getInstanceDescriptor(),
                                monkeyConnectionsArray[i].getHostPort(),
                                monkeyConnectionsArray[i].getProperties(),
                                monkeyConnectionsArray[i].getMonkeys());
                        connectionsTemp.add(newConnectionDescriptor);
                    }

                    monkeyConnections = connectionsTemp;
                } else if (value.strategyKey == ChaosStrategyKey.RANDOM_NETWORK_PARTITION) {
                    LOG.debug("Retrieve array of instance ids");
                    Set<InstanceKey> instances = new HashSet<>();
                    monkeyConnections.forEach(connectionDescriptor ->
                            instances.add(new InstanceKey(connectionDescriptor.getInstanceDescriptor().instanceKey)));

                    LOG.debug("Retrieve chaos state for serviceKey:{}", instance.serviceKey);
                    ConcurrentNavigableMap<ChaosStateKey, TimestampedValue<ChaosState>> gotChaosStates =
                            upenaStore.chaosStates.find(new ChaosStateFilter(
                                    instance.serviceKey,
                                    0, Integer.MAX_VALUE));

                    if (gotChaosStates.size() == 1) {
                        ChaosStateKey chaosStateKey = gotChaosStates.firstEntry().getKey();
                        ChaosState chaosStateValue = gotChaosStates.firstEntry().getValue().getValue();

                        if (ChaosStateHelper.instancesMatch(chaosStateValue.instanceRoutes, instances) ||
                                ChaosStateHelper.propertiesMatch(chaosStateValue.properties, value.properties)) {
                            long interval = ChaosStateHelper.parseInterval(value.properties);

                            if (chaosStateValue.disableAfterTime > 0) {
                                if (chaosStateValue.disableAfterTime > System.currentTimeMillis()) {
                                    LOG.debug("Chaos State is active for serviceKey:{} {}",
                                            instance.serviceKey,
                                            chaosStateValue.instanceRoutes);

                                    Set<InstanceKey> knownInstanceRoutes = chaosStateValue.instanceRoutes.get(instanceKey);
                                    List<ConnectionDescriptor> connectionsTemp = new ArrayList<>();

                                    for (ConnectionDescriptor connectionDescriptor : monkeyConnections) {
                                        if (knownInstanceRoutes.contains(new InstanceKey(connectionDescriptor.getInstanceDescriptor().instanceKey))) {
                                            LOG.debug("Instance is in routing table {}", connectionDescriptor.getInstanceDescriptor().instanceKey);

                                            connectionsTemp.add(connectionDescriptor);
                                        } else {
                                            LOG.debug("Instance is not in routing table {}", connectionDescriptor.getInstanceDescriptor().instanceKey);

                                            ConnectionDescriptor newConnectionDescriptor = new ConnectionDescriptor(
                                                    connectionDescriptor.getInstanceDescriptor(),
                                                    new HostPort(
                                                            ChaosStateHelper.IPV4_DEV_NULL,
                                                            connectionDescriptor.getHostPort().getPort()),
                                                    connectionDescriptor.getProperties(),
                                                    connectionDescriptor.getMonkeys());

                                            connectionsTemp.add(newConnectionDescriptor);
                                        }
                                    }

                                    monkeyConnections = connectionsTemp;
                                } else {
                                    LOG.debug("Chaos State is being deactivated for serviceKey:{}", instance.serviceKey);

                                    upenaStore.chaosStates.update(
                                            chaosStateKey,
                                            new ChaosState(instance.serviceKey, System.currentTimeMillis() + interval, 0, chaosStateValue.instanceRoutes, chaosStateValue.properties));
                                }
                            } else if (chaosStateValue.enableAfterTime > 0) {
                                if (chaosStateValue.enableAfterTime > System.currentTimeMillis()) {
                                    LOG.debug("Chaos State is inactive for serviceKey:{}", instance.serviceKey);
                                } else {
                                    LOG.debug("Chaos State will be activated for serviceKey:{}", instance.serviceKey);

                                    upenaStore.chaosStates.update(
                                            chaosStateKey,
                                            new ChaosState(instance.serviceKey, 0, System.currentTimeMillis() + interval, chaosStateValue.instanceRoutes, chaosStateValue.properties));
                                }
                            } else {
                                LOG.warn("Chaos state is invalid; regenerate chaos state.");
                                regenChaosState(instance.serviceKey, instances, gotChaosStates, value.properties);
                            }
                        } else {
                            LOG.warn("Instance set or chaos properties have changed; regenerate chaos state.");
                            regenChaosState(instance.serviceKey, instances, gotChaosStates, value.properties);
                        }
                    } else {
                        LOG.info("Chaos State not found ({}) for serviceKey:{}", gotChaosStates.size(), instance.serviceKey);
                        regenChaosState(instance.serviceKey, instances, gotChaosStates, value.properties);
                    }
                } else if (value.strategyKey == ChaosStrategyKey.ADHOC_NETWORK_PARTITION) {
                    LOG.debug("Retrieve array of instance ids");
                    Set<InstanceKey> instances = new HashSet<>();
                    monkeyConnections.forEach(connectionDescriptor ->
                            instances.add(new InstanceKey(connectionDescriptor.getInstanceDescriptor().instanceKey)));

                    LOG.debug("Retrieve chaos state for serviceKey:{}", instance.serviceKey);
                    ConcurrentNavigableMap<ChaosStateKey, TimestampedValue<ChaosState>> gotChaosStates =
                            upenaStore.chaosStates.find(new ChaosStateFilter(
                                    instance.serviceKey,
                                    0, Integer.MAX_VALUE));

                    ChaosState chaosStateValue;
                    if (gotChaosStates.size() == 1) {
                        chaosStateValue = gotChaosStates.firstEntry().getValue().getValue();

                        if (!ChaosStateHelper.instancesMatch(chaosStateValue.instanceRoutes, instances)) {
                            LOG.warn("Instance set has changed; regenerate chaos state.");
                            chaosStateValue = regenChaosState(instance.serviceKey, instances, gotChaosStates, value.properties);
                        } else if (!ChaosStateHelper.propertiesMatch(chaosStateValue.properties, value.properties)) {
                            LOG.warn("Chaos properties have changed; regenerate chaos state.");
                            chaosStateValue = regenChaosState(instance.serviceKey, instances, gotChaosStates, value.properties);
                        }
                    } else {
                        LOG.info("Chaos State not found ({}) for serviceKey:{}", gotChaosStates.size(), instance.serviceKey);
                        chaosStateValue = regenChaosState(instance.serviceKey, instances, gotChaosStates, value.properties);
                    }

                    Set<InstanceKey> knownInstanceRoutes = chaosStateValue.instanceRoutes.get(instanceKey);
                    List<ConnectionDescriptor> connectionsTemp = new ArrayList<>();

                    for (ConnectionDescriptor connectionDescriptor : monkeyConnections) {
                        if (knownInstanceRoutes.contains(new InstanceKey(connectionDescriptor.getInstanceDescriptor().instanceKey))) {
                            LOG.debug("Instance is in routing table {}", connectionDescriptor.getInstanceDescriptor().instanceKey);

                            connectionsTemp.add(connectionDescriptor);
                        } else {
                            LOG.debug("Instance is not in routing table {}", connectionDescriptor.getInstanceDescriptor().instanceKey);

                            ConnectionDescriptor newConnectionDescriptor = new ConnectionDescriptor(
                                    connectionDescriptor.getInstanceDescriptor(),
                                    new HostPort(
                                            ChaosStateHelper.IPV4_DEV_NULL,
                                            connectionDescriptor.getHostPort().getPort()),
                                    connectionDescriptor.getProperties(),
                                    connectionDescriptor.getMonkeys());

                            connectionsTemp.add(newConnectionDescriptor);
                        }
                    }

                    monkeyConnections = connectionsTemp;
                }
            }
        }

        return monkeyConnections;
    }

    private ChaosState regenChaosState(ServiceKey serviceKey,
                                       Set<InstanceKey> instances,
                                       ConcurrentNavigableMap<ChaosStateKey, TimestampedValue<ChaosState>> chaosStates,
                                       Map<String, String> properties) throws Exception {
        LOG.info("Generate chaos state for serviceKey:{} {} {}",
                serviceKey, instances, properties);

        // remove all (just in case)
        for (ChaosStateKey chaosStateKey : chaosStates.keySet()) {
            upenaStore.chaosStates.remove(chaosStateKey);
        }

        ChaosState res = ChaosStateGenerator.create(serviceKey, instances, properties);
        upenaStore.chaosStates.update(null, res);

        return res;
    }

    private String randomizeHost(String existingHost) {
        StringBuilder randHost = new StringBuilder(existingHost.length());

        Matcher matcher = ipPattern.matcher(existingHost);
        if (matcher.matches()) {
            List<String> octets = Arrays.asList(existingHost.split("\\."));
            Collections.shuffle(octets);
            octets.forEach(octet -> {
                if (randHost.length() > 0) {
                    randHost.append(".");
                }
                randHost.append(octet);
            });
        } else {
            List<String> letters = Arrays.asList(existingHost.split(""));
            Collections.shuffle(letters);
            letters.forEach(randHost::append);
        }

        return randHost.toString();
    }

    private int randomizePort(int existingPort) {
        int iter = 5;
        int randPort = (int) (Math.random() * (Short.MAX_VALUE - 10_000)) + 10_000;
        while (randPort == existingPort && --iter > 0) {
            randPort = (int) (Math.random() * (Short.MAX_VALUE - 10_000)) + 10_000;
        }
        return randPort;
    }

}
