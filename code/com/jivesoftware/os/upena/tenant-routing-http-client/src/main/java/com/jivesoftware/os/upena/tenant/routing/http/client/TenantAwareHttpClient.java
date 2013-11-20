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
