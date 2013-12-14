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
import java.util.concurrent.ConcurrentHashMap;

public class TenantRoutingClient<T, C> {

    static private final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final TenantsServiceConnectionDescriptorProvider connectionPoolProvider;
    private final ClientConnectionsFactory<C> clientConnectionsFactory;
    private final ClientCloser<C> clientCloser;
    private final ConcurrentHashMap<T, TimestampedClient<C>> tenantsHttpClient = new ConcurrentHashMap<>();

    public TenantRoutingClient(TenantsServiceConnectionDescriptorProvider connectionPoolProvider,
            ClientConnectionsFactory<C> clientConnectionsFactory,
            ClientCloser<C> clientCloser) {
        this.connectionPoolProvider = connectionPoolProvider;
        this.clientConnectionsFactory = clientConnectionsFactory;
        this.clientCloser = clientCloser;
    }

    public <R, E extends Throwable> R tenantAwareCall(T tenant, ClientCall<C, R, E> clientCall) throws E {
        ConnectionDescriptors connections = connectionPoolProvider.getConnections(tenant);
        TimestampedClient<C> timestampedClient = tenantsHttpClient.get(tenant);
        if (timestampedClient == null || timestampedClient.getTimestamp() < connections.getTimestamp()) {
            if (timestampedClient != null) {
                try {
                    clientCloser.closeClient(timestampedClient.getClient());
                } catch (Exception x) {
                    LOG.warn("Failed while trying to close client:" + timestampedClient.getClient(), x);
                }
            }
            C client = clientConnectionsFactory.createClient(connections);
            if (client == null) {
                throw new IllegalStateException("clientConnectionsFactory:" + clientConnectionsFactory + " should not return a null client but did!");
            }
            timestampedClient = new TimestampedClient(System.currentTimeMillis(), client);
            tenantsHttpClient.put(tenant, timestampedClient);
        }
        return clientCall.call(timestampedClient.getClient());
    }

    public void closeAll() {
        // TODO??
    }
}
