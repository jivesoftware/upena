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
            timestampedClient = new TimestampedClient(System.currentTimeMillis(), client);
            tenantsHttpClient.put(tenant, timestampedClient);
        }
        return clientCall.call(timestampedClient.getClient());
    }

    public void closeAll() {
        // TODO??
    }
}
