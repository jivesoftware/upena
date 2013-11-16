package com.jivesoftware.os.upena.tenant.routing.http.client;

import com.jivesoftware.os.jive.utils.http.client.HttpClientException;
import com.jivesoftware.os.jive.utils.http.client.HttpResponse;

public interface TenantAwareHttpClient<T> {

    /**
     *
     * @param path everything but the leading "http/s://host:port"
     * @return
     * @throws HttpClientException
     */
    HttpResponse get(T tenant, String path) throws HttpClientException;

    /**
     *
     * @param path everything but the leading "http/s://host:port"
     * @param postJsonBody
     * @return
     * @throws HttpClientException
     */
    HttpResponse postJson(T tenant, String path, String postJsonBody) throws HttpClientException;

    /**
     *
     * @param path everything but the leading "http/s://host:port"
     * @param postBytes
     * @return
     * @throws HttpClientException
     */
    HttpResponse postBytes(T tenant, String path, byte[] postBytes) throws HttpClientException;
}
