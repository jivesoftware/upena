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
package com.jivesoftware.os.upena.routing.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryConnectionsDescriptorsProvider implements ConnectionDescriptorsProvider {

    private final List<ConnectionDescriptor> defaultConnectionDescriptor;
    private final Set<String> routingKeys = new HashSet<>();
    private final ConcurrentHashMap<String, ConnectionDescriptor> routings = new ConcurrentHashMap<>();

    public InMemoryConnectionsDescriptorsProvider(List<ConnectionDescriptor> defaultConnectionDescriptor) {
        this.defaultConnectionDescriptor = defaultConnectionDescriptor;
    }

    private String key(String tenantId, String instanceId, String connectToServiceNamed, String portName) {
        return "tenantId=" + tenantId + "&instanceid=" + instanceId + "&connectToServiceNamed=" + connectToServiceNamed + "&portName=" + portName;
    }

    public void set(String tenantId, String instanceId, String connectToServiceNamed, String portName, ConnectionDescriptor connectionDescriptor) {
        String key = key(tenantId, instanceId, connectToServiceNamed, portName);
        routings.put(key, connectionDescriptor);
    }

    public ConnectionDescriptor get(String tenantId, String instanceId, String connectToServiceNamed, String portName) {
        return routings.get(key(tenantId, instanceId, connectToServiceNamed, portName));
    }

    public void clear(String tenantId, String instanceId, String connectToServiceNamed, String portName) {
        routings.remove(key(tenantId, instanceId, connectToServiceNamed, portName));
    }

    public Collection<String> getRequestedRoutingKeys() {
        return Arrays.asList(routingKeys.toArray(new String[routingKeys.size()]));
    }

    @Override
    public ConnectionDescriptorsResponse requestConnections(ConnectionDescriptorsRequest connectionsRequest) {
        String key = key(connectionsRequest.getTenantId(),
                connectionsRequest.getInstanceId(),
                connectionsRequest.getConnectToServiceNamed(),
                connectionsRequest.getPortName());
        routingKeys.add(key);
        ConnectionDescriptor connectionDescriptor = routings.get(key);
        List<ConnectionDescriptor> connectionDescriptors = new ArrayList<>();
        String releaseGroup;
        if (connectionDescriptor == null) {
            releaseGroup = "default";
            if (defaultConnectionDescriptor != null) {
                connectionDescriptors.addAll(defaultConnectionDescriptor);
            }
        } else {
            releaseGroup = "overriden";
            connectionDescriptors.add(connectionDescriptor);
        }
        return new ConnectionDescriptorsResponse(1, Arrays.asList("Success"), releaseGroup, connectionDescriptors);
    }
}
