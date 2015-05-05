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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.MockitoAnnotations.initMocks;

public class TenantRoutingClientTest {

    @Mock
    private ClientCall<TestClient, Boolean, IOException> clientCall;
    @Mock
    private ClientCloser closer;
    @Mock
    private TenantsServiceConnectionDescriptorProvider tenantsServiceConnectionDescriptorProvider;
    @Mock
    private ClientConnectionsFactory<TestClient> clientConnectionsFactory;
    private String tenantId = "testTenant";
    private TestClient testClient = new TestClient();
    private ConnectionDescriptors connectionDescriptors;

    @BeforeMethod
    public void setUp() {
        initMocks(this);
        try {
            Mockito.when(clientCall.call(testClient)).thenReturn(Boolean.TRUE);
        } catch (IOException ex) {
            Assert.fail();
        }

        initDescriptorsPool(System.currentTimeMillis());
    }

    private void initDescriptorsPool(long timestamp) {
        ConnectionDescriptor descriptor = new ConnectionDescriptor("localhost", 7777, Collections.EMPTY_MAP);
        connectionDescriptors = new ConnectionDescriptors(timestamp, Arrays.asList(descriptor));
        Mockito.when(tenantsServiceConnectionDescriptorProvider.getConnections(tenantId)).thenReturn(connectionDescriptors);
        Mockito.when(clientConnectionsFactory.createClient(connectionDescriptors)).thenReturn(testClient);
    }

    @Test
    public void testTenantAwareCall() throws Exception {
        TenantRoutingClient<String, TestClient> instance = new TenantRoutingClient<>(
                tenantsServiceConnectionDescriptorProvider, clientConnectionsFactory, closer);
        Boolean expResult = true;
        Boolean result = instance.tenantAwareCall(tenantId, clientCall);

        Assert.assertEquals(result, expResult);
        Mockito.verifyZeroInteractions(closer);

        initDescriptorsPool(System.currentTimeMillis() + 1000);

        result = instance.tenantAwareCall(tenantId, clientCall);
        Assert.assertEquals(result, expResult);
        Mockito.verify(closer).closeClient(testClient);
    }

    private static class TestClient {
    }
}