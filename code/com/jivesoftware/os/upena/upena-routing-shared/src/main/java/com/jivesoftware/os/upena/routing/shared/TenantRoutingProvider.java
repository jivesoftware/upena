package com.jivesoftware.os.upena.routing.shared;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class TenantRoutingProvider {

    private final ConcurrentHashMap<String, TenantsServiceConnectionDescriptorProvider> serviceConnectionDescriptorsProvider = new ConcurrentHashMap<>();
    private final String instanceId;
    private final ConnectionDescriptorsProvider connectionsDescriptorProvider;

    public TenantRoutingProvider(String instanceId, ConnectionDescriptorsProvider connectionsDescriptorProvider) {
        this.instanceId = instanceId;
        this.connectionsDescriptorProvider = connectionsDescriptorProvider;
    }

    private String key(String connectToService, String portName) {
        return connectToService + "." + portName;
    }

    public void invalidateAll() {
        for (TenantsServiceConnectionDescriptorProvider v : serviceConnectionDescriptorsProvider.values()) {
            v.invalidateAll();
        }
    }

    public void invalidateTenant(String connectToService, String portName, String tenantId) {
        TenantsServiceConnectionDescriptorProvider got = serviceConnectionDescriptorsProvider.get(key(connectToService, portName));
        if (got != null) {
            got.invalidateTenant(tenantId);
        }
    }

    public TenantsRoutingReport getRoutingReport() {
        TenantsRoutingReport report = new TenantsRoutingReport();
        for (Entry<String, TenantsServiceConnectionDescriptorProvider> e : serviceConnectionDescriptorsProvider.entrySet()) {
            report.serviceReport.put(e.getKey(), e.getValue().getRoutingReport());
        }
        return report;
    }

    public TenantsServiceConnectionDescriptorProvider getConnections(String connectToServiceNamed, String portName) {
        if (connectToServiceNamed == null) {
            return null;
        }
        if (portName == null) {
            return null;
        }
        String key = key(connectToServiceNamed, portName);
        TenantsServiceConnectionDescriptorProvider got = serviceConnectionDescriptorsProvider.get(key);
        if (got != null) {
            return got;
        }
        got = new TenantsServiceConnectionDescriptorProvider(instanceId, connectionsDescriptorProvider, connectToServiceNamed, portName);
        TenantsServiceConnectionDescriptorProvider had = serviceConnectionDescriptorsProvider.putIfAbsent(key(connectToServiceNamed, portName), got);
        if (had != null) {
            got = had;
        }
        return got;
    }
}
