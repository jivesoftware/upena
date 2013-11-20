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

import com.jivesoftware.os.jive.utils.logger.MetricLogger;
import com.jivesoftware.os.jive.utils.logger.MetricLoggerFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TenantsServiceConnectionDescriptorProvider<T> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final String instanceId;
    private final ConnectionDescriptorsProvider connectionsProvider;
    private final String connectToServiceNamed;
    private final String portName;
    private final Map<String, ConnectionDescriptors> userIdsConnectionDescriptors = new ConcurrentHashMap<>();
    private final Map<T, String> tenantToUserId = new ConcurrentHashMap<>();

    public TenantsServiceConnectionDescriptorProvider(String instanceId,
            ConnectionDescriptorsProvider connectionsProvider,
            String connectToServiceNamed,
            String portName) {
        this.instanceId = instanceId;
        this.connectionsProvider = connectionsProvider;
        this.connectToServiceNamed = connectToServiceNamed;
        this.portName = portName;
    }

    void invalidateAll() {
        tenantToUserId.clear();
        userIdsConnectionDescriptors.clear();
    }

    public void invalidateTenant(T tenantId) {
        tenantToUserId.remove(tenantId);
    }

    public TenantsRoutingServiceReport getRoutingReport() {
        TenantsRoutingServiceReport report = new TenantsRoutingServiceReport();
        report.tenantToUserId.putAll(tenantToUserId);
        report.userIdsConnectionDescriptors.putAll(userIdsConnectionDescriptors);
        return report;
    }

    public ConnectionDescriptors getConnections(T tenantId) {
        if (tenantId == null) {
            return new ConnectionDescriptors(System.currentTimeMillis(), Collections.<ConnectionDescriptor>emptyList());
        }
        ConnectionDescriptors connections;
        String userId = tenantToUserId.get(tenantId);
        if (userId != null) {
            connections = userIdsConnectionDescriptors.get(userId);
            if (connections == null) {
                connections = new ConnectionDescriptors(System.currentTimeMillis(), Collections.<ConnectionDescriptor>emptyList());
            }
        } else {
            ConnectionDescriptorsResponse connectionsResponse = connectionsProvider.requestConnections(new ConnectionDescriptorsRequest(
                    tenantId.toString(), instanceId, connectToServiceNamed, portName));
            if (connectionsResponse == null) {
                userId = "unknown";
                connections = new ConnectionDescriptors(System.currentTimeMillis(), Collections.<ConnectionDescriptor>emptyList());
            } else if (connectionsResponse.getReturnCode() < 0) {
                userId = "unknown";
                LOG.warn(Arrays.deepToString(connectionsResponse.getMessages().toArray()));
                connections = new ConnectionDescriptors(System.currentTimeMillis(), Collections.<ConnectionDescriptor>emptyList());
            } else {
                userId = connectionsResponse.getUserId();
                connections = new ConnectionDescriptors(System.currentTimeMillis(), connectionsResponse.getConnections());
                cleanupConnections(new HashSet(connectionsResponse.getValidUserIds()));
            }
            tenantToUserId.put(tenantId, userId);
            userIdsConnectionDescriptors.put(userId, connections);
        }
        return connections;
    }

    private void cleanupConnections(HashSet<String> validUserIds) {
        Set<T> removedTenants = new HashSet<>();
        Set<String> removedUsers = new HashSet<>();
        for (Entry<T, String> e : tenantToUserId.entrySet()) {
            if (!validUserIds.contains(e.getValue())) {
                removedTenants.add(e.getKey());
                removedUsers.add(e.getValue());
            }
        }
        for (T removedTenant : removedTenants) {
            tenantToUserId.remove(removedTenant);
        }

        for (String removedUser : removedUsers) {
            userIdsConnectionDescriptors.remove(removedUser);
        }
    }
}
