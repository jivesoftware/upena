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
import com.jivesoftware.os.jive.utils.http.client.HttpStreamResponse;
import com.jivesoftware.os.upena.routing.shared.TenantRoutingClient;
import java.util.Map;

public class TenantRoutingHttpClient<T> implements TenantAwareHttpClient<T> {

    private final TenantRoutingClient<T, HttpClient> tenantRoutingClient;

    public TenantRoutingHttpClient(TenantRoutingClient<T, HttpClient> tenantRoutingClient) {
        this.tenantRoutingClient = tenantRoutingClient;
    }

    @Override
    public HttpResponse get(T tenant, final String path) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.get(path));
    }

    @Override
    public HttpResponse postJson(T tenant, final String path, final String postJsonBody) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.postJson(path, postJsonBody));
    }

    @Override
    public HttpResponse postBytes(T tenant, final String path, final byte[] postBytes) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.postBytes(path, postBytes));
    }

    @Override
    public HttpResponse get(T tenant, final String path, final Map<String, String> headers) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.get(path, headers));
    }

    @Override
    public HttpStreamResponse getStream(T tenant, final String path) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.getStream(path));
    }

    @Override
    public HttpResponse get(T tenant, final String path, final int timeoutMillis) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.get(path, timeoutMillis));
    }

    @Override
    public HttpResponse get(T tenant, final String path, final Map<String, String> headers, final int timeoutMillis) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.get(path, headers, timeoutMillis));
    }

    @Override
    public HttpStreamResponse getStream(T tenant, final String path, final int timeoutMillis) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.getStream(path, timeoutMillis));
    }

    @Override
    public HttpResponse delete(T tenant, final String path) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.get(path));
    }

    @Override
    public HttpResponse delete(T tenant, final String path, final Map<String, String> headers) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.delete(path, headers));
    }

    @Override
    public HttpStreamResponse deleteStream(T tenant, final String path) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.deleteStream(path));
    }

    @Override
    public HttpResponse delete(T tenant, final String path, final int timeoutMillis) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.delete(path, timeoutMillis));
    }

    @Override
    public HttpResponse delete(T tenant, final String path, final Map<String, String> headers, final int timeoutMillis) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.delete(path, headers, timeoutMillis));
    }

    @Override
    public HttpStreamResponse deleteStream(T tenant, final String path, final int timeoutMillis) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.deleteStream(path, timeoutMillis));
    }

    @Override
    public HttpResponse postJson(T tenant, final String path, final String string1, final Map<String, String> headers) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.postJson(path, string1, headers));
    }

    @Override
    public HttpResponse postJson(T tenant, final String path, final String string1, final int timeoutMillis) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.postJson(path, string1, timeoutMillis));
    }

    @Override
    public HttpResponse postJson(T tenant, final String path, final String string1, final Map<String, String> headers, final int timeoutMillis)
        throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.postJson(path, string1, headers, timeoutMillis));
    }

    @Override
    public HttpResponse postBytes(T tenant, final String path, final byte[] bytes, final int timeoutMillis) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.postBytes(path, bytes, timeoutMillis));
    }

    @Override
    public HttpResponse putJson(T tenant, final String path, final String putJsonBody) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.putJson(path, putJsonBody));
    }

    @Override
    public HttpResponse putJson(T tenant, final String path, final String putJsonBody, final Map<String, String> headers) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.putJson(path, putJsonBody, headers));
    }

    @Override
    public HttpResponse putJson(T tenant, final String path, final String putJsonBody, final int timeoutMillis) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.putJson(path, putJsonBody, timeoutMillis));
    }

    @Override
    public HttpResponse putJson(T tenant, final String path, final String putJsonBody, final Map<String, String> headers, final int timeoutMillis)
        throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.putJson(path, putJsonBody, headers, timeoutMillis));
    }

    @Override
    public HttpResponse putBytes(T tenant, final String path, final byte[] bytes) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.putBytes(path, bytes));
    }

    @Override
    public HttpResponse putBytes(T tenant, final String path, final byte[] bytes, final int timeoutMillis) throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, (HttpClient client) -> client.putBytes(path, bytes, timeoutMillis));
    }

}
