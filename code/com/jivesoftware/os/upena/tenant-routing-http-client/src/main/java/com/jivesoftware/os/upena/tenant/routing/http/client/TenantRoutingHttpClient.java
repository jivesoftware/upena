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
import com.jivesoftware.os.jive.utils.http.client.HttpClientException;
import com.jivesoftware.os.jive.utils.http.client.HttpResponse;
import com.jivesoftware.os.upena.routing.shared.ClientCall;
import com.jivesoftware.os.upena.routing.shared.TenantRoutingClient;


public class TenantRoutingHttpClient<T> implements TenantAwareHttpClient<T> {

    private final TenantRoutingClient<T, HttpClient> tenantRoutingClient;

    public TenantRoutingHttpClient(TenantRoutingClient<T, HttpClient> tenantRoutingClient) {
        this.tenantRoutingClient = tenantRoutingClient;
    }

    @Override
    public HttpResponse get(T tenant, final String path) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, new ClientCall<HttpClient, HttpResponse, HttpClientException>() {
            @Override
            public HttpResponse call(HttpClient client) throws HttpClientException {
                return client.get(path);
            }
        });
    }

    @Override
    public HttpResponse postJson(T tenant, final String path, final String postJsonBody) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, new ClientCall<HttpClient, HttpResponse, HttpClientException>() {
            @Override
            public HttpResponse call(HttpClient client) throws HttpClientException {
                return client.postJson(path, postJsonBody);
            }
        });
    }

    @Override
    public HttpResponse postBytes(T tenant, final String path, final byte[] postBytes) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, new ClientCall<HttpClient, HttpResponse, HttpClientException>() {
            @Override
            public HttpResponse call(HttpClient client) throws HttpClientException {
                return client.postBytes(path, postBytes);
            }
        });
    }
}
