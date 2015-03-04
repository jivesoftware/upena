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
package com.jivesoftware.os.upena.tenant.routing.http.client;

import com.jivesoftware.os.jive.utils.http.client.HttpClient;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfig;
import com.jivesoftware.os.jive.utils.http.client.HttpClientConfiguration;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactory;
import com.jivesoftware.os.jive.utils.http.client.HttpClientFactoryProvider;
import com.jivesoftware.os.jive.utils.http.client.NoClientAvailableHttpClient;
import com.jivesoftware.os.upena.routing.shared.ClientCloser;
import com.jivesoftware.os.upena.routing.shared.ClientConnectionsFactory;
import com.jivesoftware.os.upena.routing.shared.ConnectionDescriptor;
import com.jivesoftware.os.upena.routing.shared.ConnectionDescriptors;
import com.jivesoftware.os.upena.routing.shared.TenantRoutingClient;
import com.jivesoftware.os.upena.routing.shared.TenantsServiceConnectionDescriptorProvider;
import java.util.ArrayList;
import java.util.List;


public class TenantRoutingHttpClientInitializer<T> {

    public TenantAwareHttpClient<T> initialize(TenantsServiceConnectionDescriptorProvider<T> connectionPoolProvider) {


        ClientConnectionsFactory<HttpClient> clientConnectionsFactory = new ClientConnectionsFactory<HttpClient>() {
            @Override
            public HttpClient createClient(ConnectionDescriptors connectionDescriptors) {
                List<HttpClient> httpClients = new ArrayList<>();
                HttpClientFactoryProvider httpClientFactoryProvider = new HttpClientFactoryProvider();
                for (ConnectionDescriptor connection : connectionDescriptors.getConnectionDescriptors()) {
                    List<HttpClientConfiguration> config = new ArrayList<>();
                    config.add(HttpClientConfig.newBuilder().setSocketTimeoutInMillis(15000).build());
                    HttpClientFactory createHttpClientFactory = httpClientFactoryProvider.createHttpClientFactory(config);
                    HttpClient httpClient = createHttpClientFactory.createClient(connection.getHost(), connection.getPort());
                    httpClients.add(httpClient);
                }

                if (httpClients.isEmpty()) {
                    return new NoClientAvailableHttpClient();
                } else {
                    return new RoundRobinHttpClient(httpClients.toArray(new HttpClient[httpClients.size()]), 10, 10000);
                }
            }
        };

        ClientCloser<HttpClient> clientCloser = new ClientCloser<HttpClient>() {
            @Override
            public void closeClient(HttpClient client) {
            }
        };

        TenantRoutingClient<T, HttpClient> tenantRoutingClient = new TenantRoutingClient<>(connectionPoolProvider,
                clientConnectionsFactory, clientCloser);
        return new TenantRoutingHttpClient<>(tenantRoutingClient);
    }
}
