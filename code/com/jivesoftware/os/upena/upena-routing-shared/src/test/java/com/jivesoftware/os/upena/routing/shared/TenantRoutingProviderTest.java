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

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TenantRoutingProviderTest {

    private ConnectionDescriptorsProvider connectionDescriptorsProvider;
    private TenantRoutingProvider provider;

    @BeforeMethod
    public void setUp() {
        connectionDescriptorsProvider = Mockito.mock(ConnectionDescriptorsProvider.class);
        provider = new TenantRoutingProvider("1234", connectionDescriptorsProvider);
    }

    @Test
    public void testGetConnections() throws Exception {

        ConnectionDescriptorsRequest request = new ConnectionDescriptorsRequest("tenant", "1234", "serviceA", "port1");
        ConnectionDescriptorsResponse response = new ConnectionDescriptorsResponse(0, null, null, null, null);
        Mockito.when(connectionDescriptorsProvider.requestConnections(Mockito.eq(request))).thenReturn(response);

        TenantsServiceConnectionDescriptorProvider connections = provider.getConnections("invalid", null);
        Assert.assertNull(connections);

        connections = provider.getConnections(null, "invalid");
        Assert.assertNull(connections);

        connections = provider.getConnections(null, null);
        Assert.assertNull(connections);

        connections = provider.getConnections("serviceA", "port1");
        Assert.assertNotNull(connections);


    }

    @Test
    public void testGetRoutingReport() throws Exception {
        TenantsRoutingReport routingReport = provider.getRoutingReport();
        Assert.assertTrue(routingReport.serviceReport.isEmpty());

        ConnectionDescriptorsRequest request = new ConnectionDescriptorsRequest("tenant", "1234", "serviceA", "port1");
        ConnectionDescriptorsResponse response = new ConnectionDescriptorsResponse(0, null, null, null, null);
        Mockito.when(connectionDescriptorsProvider.requestConnections(Mockito.eq(request))).thenReturn(response);

        TenantsServiceConnectionDescriptorProvider connections = provider.getConnections("serviceA", "port1");
        routingReport = provider.getRoutingReport();
        Assert.assertTrue(routingReport.serviceReport.size() == 1);

    }
}