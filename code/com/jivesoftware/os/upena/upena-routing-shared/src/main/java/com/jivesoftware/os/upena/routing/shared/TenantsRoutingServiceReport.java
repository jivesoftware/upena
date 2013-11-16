package com.jivesoftware.os.upena.routing.shared;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TenantsRoutingServiceReport<T> {

    public Map<String, ConnectionDescriptors> userIdsConnectionDescriptors = new ConcurrentHashMap<>();
    public Map<T, String> tenantToUserId = new ConcurrentHashMap<>();
}
