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
import com.jivesoftware.os.routing.bird.shared.HostPort;
import com.jivesoftware.os.upena.shared.ChaosStrategyKey;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Monkey;
import com.jivesoftware.os.upena.shared.MonkeyFilter;
import com.jivesoftware.os.upena.shared.MonkeyKey;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChaosService {

    private final UpenaStore upenaStore;

    private static final String IPADDRESS_PATTERN =
            "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$";
    private static final Pattern ipPattern = Pattern.compile(IPADDRESS_PATTERN);

    public ChaosService(UpenaStore upenaStore) {
        this.upenaStore = upenaStore;
    }

    String monkeyAffect(Instance instance) throws Exception {
        String affect = "";

        ConcurrentNavigableMap<MonkeyKey, TimestampedValue<Monkey>> gotMonkeys =
                findMonkeys(instance.clusterKey, instance.hostKey, instance.serviceKey);

        String prefix = "'";
        String postfix = "'";
        for (Map.Entry<MonkeyKey, TimestampedValue<Monkey>> entry : gotMonkeys.entrySet()) {
            Monkey value = entry.getValue().getValue();

            if (value.enabled) {
                if (!affect.isEmpty()) {
                    prefix = ", '";
                }
                affect += prefix + value.strategyKey.name + postfix;
            }
        }

        return affect;
    }

    List<ConnectionDescriptor> unleashMonkey(Instance instance, List<ConnectionDescriptor> connections) throws Exception {
        List<ConnectionDescriptor> monkeyConnections = connections;

        ConcurrentNavigableMap<MonkeyKey, TimestampedValue<Monkey>> gotMonkeys =
                findMonkeys(instance.clusterKey, instance.hostKey, instance.serviceKey);

        for (Map.Entry<MonkeyKey, TimestampedValue<Monkey>> entry : gotMonkeys.entrySet()) {
            Monkey value = entry.getValue().getValue();

            if (value.enabled) {
                if (value.strategyKey == ChaosStrategyKey.RANDOMIZE_HOSTNAME) {
                    List<ConnectionDescriptor> connectionsTemp = new ArrayList<>();

                    for (ConnectionDescriptor connectionDescriptor : monkeyConnections) {
                        Map<String, String> newMonkeys =
                                (connectionDescriptor.getMonkeys() != null) ? connectionDescriptor.getMonkeys() : new HashMap<>();
                        newMonkeys.put(ChaosStrategyKey.RANDOMIZE_HOSTNAME.key,
                                ChaosStrategyKey.RANDOMIZE_HOSTNAME.name);
                        HostPort newHostPort = new HostPort(
                                randomizeHost(connectionDescriptor.getHostPort().getHost()),
                                connectionDescriptor.getHostPort().getPort());

                        ConnectionDescriptor newConnectionDescriptor = new ConnectionDescriptor(
                                connectionDescriptor.getInstanceDescriptor(),
                                newHostPort,
                                connectionDescriptor.getProperties(),
                                newMonkeys);
                        connectionsTemp.add(newConnectionDescriptor);
                    }

                    monkeyConnections = connectionsTemp;
                } else if (value.strategyKey == ChaosStrategyKey.RANDOMIZE_PORT) {
                    List<ConnectionDescriptor> connectionsTemp = new ArrayList<>();

                    for (ConnectionDescriptor connectionDescriptor : monkeyConnections) {
                        Map<String, String> newMonkeys = (connectionDescriptor.getMonkeys() != null) ? connectionDescriptor.getMonkeys() : new HashMap<>();
                        newMonkeys.put(ChaosStrategyKey.RANDOMIZE_PORT.key,
                                ChaosStrategyKey.RANDOMIZE_PORT.name);
                        HostPort newHostPort = new HostPort(
                                connectionDescriptor.getHostPort().getHost(),
                                randomizePort(connectionDescriptor.getHostPort().getPort()));

                        ConnectionDescriptor newConnectionDescriptor = new ConnectionDescriptor(
                                connectionDescriptor.getInstanceDescriptor(),
                                newHostPort,
                                connectionDescriptor.getProperties(),
                                newMonkeys);
                        connectionsTemp.add(newConnectionDescriptor);
                    }

                    monkeyConnections = connectionsTemp;
                }
            }
        }

        return monkeyConnections;
    }

    ConcurrentNavigableMap<MonkeyKey, TimestampedValue<Monkey>> findMonkeys(ClusterKey clusterKey,
                                                                            HostKey hostKey,
                                                                            ServiceKey serviceKey) throws Exception {
        return upenaStore.monkeys.find(new MonkeyFilter(
                clusterKey, hostKey, serviceKey,
                null, 0, 100_000));
    }

    private String randomizeHost(String existingHost) {
        StringBuilder randHost = new StringBuilder(existingHost.length());

        Matcher matcher = ipPattern.matcher(existingHost);
        if (matcher.matches()) {
            List<String> octets = Arrays.asList(existingHost.split("\\."));
            Collections.shuffle(octets);

            for (String octet : octets) {
                if (randHost.length() > 0) {
                    randHost.append(".");
                }
                randHost.append(octet);
            }
        } else {
            List<String> letters = Arrays.asList(existingHost.split(""));
            Collections.shuffle(letters);

            for (String letter : letters) {
                randHost.append(letter);
            }
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
