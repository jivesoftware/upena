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
import com.jivesoftware.os.upena.service.UpenaTable.UpenaValueValidator;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;

public class InstanceValidator implements UpenaValueValidator<InstanceKey, Instance> {

    private final int minPort = 10000;
    private final int maxPort = Short.MAX_VALUE;

    @Override
    public Instance valiadate(UpenaTable<InstanceKey, Instance> table, InstanceKey key, Instance value) throws Exception {
        Set<Integer> usedPorts = usedPorts(table, value.hostKey, key);
        return populatePorts(usedPorts, value);
    }

    int nextFreePort(Set<Integer> usedPorts, int lastPort) {
        for (int i = lastPort; i < maxPort; i++) {
            if (!usedPorts.contains(i)) {
                usedPorts.add(i);
                return i;
            }
        }
        return -1;
    }

    Instance populatePorts(Set<Integer> usedPorts, Instance value) {
        int lastPort = minPort;
        for (String portName : new String[]{Instance.PORT_MAIN, Instance.PORT_MANAGE, Instance.PORT_JMX, Instance.PORT_DEBUG}) {
            Instance.Port port = value.ports.get(portName);
            if (port == null) {
                port = new Instance.Port();
                port.properties.put("connectionTimeoutMillis", "30000");
                port.port = nextFreePort(usedPorts, lastPort);
                lastPort = port.port;
                value.ports.put(portName, port);
            } else {
                if (usedPorts.contains(port.port)) {
                    port.port = nextFreePort(usedPorts, lastPort);
                    lastPort = port.port;
                }
            }
        }
        return value;
    }

    Set<Integer> usedPorts(UpenaTable<InstanceKey, Instance> table, HostKey hostKey, InstanceKey excludeThisInstanceKey) throws Exception {
        Set<Integer> usedPorts = new HashSet<>();
        InstanceFilter impactedFilter = new InstanceFilter(null, hostKey, null, null, null, 0, Integer.MAX_VALUE);
        ConcurrentNavigableMap<InstanceKey, TimestampedValue<Instance>> got = table.find(impactedFilter);
        for (Map.Entry<InstanceKey, TimestampedValue<Instance>> e : got.entrySet()) {
            if (!excludeThisInstanceKey.equals(e.getKey())) {
                for (Instance.Port port : e.getValue().getValue().ports.values()) {
                    usedPorts.add(port.port);
                }
            }
        }
        return usedPorts;
    }
}
