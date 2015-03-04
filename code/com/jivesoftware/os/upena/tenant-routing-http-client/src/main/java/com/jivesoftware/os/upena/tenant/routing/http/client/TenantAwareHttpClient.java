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

import com.jivesoftware.os.jive.utils.http.client.HttpClientException;
import com.jivesoftware.os.jive.utils.http.client.HttpResponse;
import com.jivesoftware.os.jive.utils.http.client.HttpStreamResponse;
import java.util.Map;

public interface TenantAwareHttpClient<T> {

    HttpResponse get(T tenant, String path) throws HttpClientException;

    HttpResponse get(T tenant, String path, Map<String, String> headers) throws HttpClientException;

    HttpStreamResponse getStream(T tenant, String path) throws HttpClientException;

    HttpResponse get(T tenant, String path, int timeoutMillis) throws HttpClientException;

    HttpResponse get(T tenant, String path, Map<String, String> headers, int timeoutMillis) throws HttpClientException;

    HttpStreamResponse getStream(T tenant, String path, int timeoutMillis) throws HttpClientException;

    HttpResponse delete(T tenant, String path) throws HttpClientException;

    HttpResponse delete(T tenant, String path, Map<String, String> headers) throws HttpClientException;

    HttpStreamResponse deleteStream(T tenant, String path) throws HttpClientException;

    HttpResponse delete(T tenant, String path, int timeoutMillis) throws HttpClientException;

    HttpResponse delete(T tenant, String path, Map<String, String> headers, int timeoutMillis) throws HttpClientException;

    HttpStreamResponse deleteStream(T tenant, String path, int timeoutMillis) throws HttpClientException;

    HttpResponse postJson(T tenant, String path, String jsonString) throws HttpClientException;

    HttpResponse postJson(T tenant, String path, String jsonString, Map<String, String> headers) throws HttpClientException;

    HttpResponse postJson(T tenant, String path, String jsonString, int timeoutMillis) throws HttpClientException;

    HttpResponse postJson(T tenant, String path, String jsonString, Map<String, String> headers, int timeoutMillis) throws HttpClientException;

    HttpResponse postBytes(T tenant, String path, byte[] bytes) throws HttpClientException;

    HttpResponse postBytes(T tenant, String path, byte[] bytes, int timeoutMillis) throws HttpClientException;

    HttpResponse putJson(T tenant, String path, String jsonString) throws HttpClientException;

    HttpResponse putJson(T tenant, String path, String jsonString, Map<String, String> headers) throws HttpClientException;

    HttpResponse putJson(T tenant, String path, String jsonString, int timeoutMillis) throws HttpClientException;

    HttpResponse putJson(T tenant, String path, String jsonString, Map<String, String> headers, int timeoutMillis) throws HttpClientException;

    HttpResponse putBytes(T tenant, String path, byte[] bytes) throws HttpClientException;

    HttpResponse putBytes(T tenant, String path, byte[] bytes, int timeoutMillis) throws HttpClientException;
}
