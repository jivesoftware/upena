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
import com.jivesoftware.os.jive.utils.logger.MetricLogger;
import com.jivesoftware.os.jive.utils.logger.MetricLoggerFactory;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RoundRobinHttpClient implements HttpClient {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final int deadAfterNErrors;
    private final long checkDeadEveryNMillis;
    private final HttpClient[] clients;
    private final AtomicInteger[] clientsErrors;
    private final AtomicLong[] clientsDeathTimestamp;
    private final AtomicInteger lastClientUsed = new AtomicInteger(0);

    public RoundRobinHttpClient(HttpClient[] clients, int deadAfterNErrors, long checkDeadEveryNMillis) {
        this.clients = clients;
        this.deadAfterNErrors = deadAfterNErrors;
        this.checkDeadEveryNMillis = checkDeadEveryNMillis;
        int l = clients.length;
        this.clientsErrors = new AtomicInteger[l];
        this.clientsDeathTimestamp = new AtomicLong[l];
        for (int i = 0; i < l; i++) {
            clientsErrors[i] = new AtomicInteger(0);
            clientsDeathTimestamp[i] = new AtomicLong(0);
        }
    }

    private HttpResponse roundRobin(HttpCall httpCall) throws HttpClientException {
        long now = System.currentTimeMillis();
        int start = lastClientUsed.get() % clients.length;
        int ci = (start + 1) % clients.length;
        while (ci != start) {
            if (clientsDeathTimestamp[ci].get() == 0 || now - clientsDeathTimestamp[ci].get() > checkDeadEveryNMillis) {
                try {
                    LOG.info("roundRobin to index:" + ci + " possibleClients:" + clients.length);
                    return httpCall.call(clients[ci]);
                } catch (HttpClientException e) {
                    if (e.getCause() instanceof IOException) {
                        if (clientsErrors[ci].incrementAndGet() > deadAfterNErrors) {
                            clientsDeathTimestamp[ci].set(now + checkDeadEveryNMillis);
                        }
                    } else {
                        throw e;
                    }
                } finally {
                    clientsDeathTimestamp[ci].set(0);
                    clientsErrors[ci].set(0);
                    int got = lastClientUsed.incrementAndGet();
                    if (got > clients.length) {
                        lastClientUsed.compareAndSet(got, 0);
                    }
                }
            }
            ci = (start + 1) % clients.length;
        }
        throw new HttpClientException("No client are available");
    }

    @Override
    public HttpResponse get(final String path) throws HttpClientException {
        return roundRobin(new HttpCall() {
            @Override
            public HttpResponse call(HttpClient client) throws HttpClientException {
                return client.get(path);
            }
        });
    }

    @Override
    public HttpResponse postJson(final String path, final String postJsonBody) throws HttpClientException {
        return roundRobin(new HttpCall() {
            @Override
            public HttpResponse call(HttpClient client) throws HttpClientException {
                return client.postJson(path, postJsonBody);
            }
        });
    }

    @Override
    public HttpResponse postBytes(final String path, final byte[] postBytes) throws HttpClientException {
        return roundRobin(new HttpCall() {
            @Override
            public HttpResponse call(HttpClient client) throws HttpClientException {
                return client.postBytes(path, postBytes);
            }
        });
    }

    @Override
    public HttpResponse get(final String path, final int socketTimeoutMillis) throws HttpClientException {
        return roundRobin(new HttpCall() {
            @Override
            public HttpResponse call(HttpClient client) throws HttpClientException {
                return client.get(path, socketTimeoutMillis);
            }
        });
    }

    @Override
    public HttpResponse postJson(final String path, final String postJsonBody, final int socketTimeoutMillis) throws HttpClientException {
        return roundRobin(new HttpCall() {
            @Override
            public HttpResponse call(HttpClient client) throws HttpClientException {
                return client.postJson(path, postJsonBody, socketTimeoutMillis);
            }
        });
    }

    @Override
    public HttpResponse postBytes(final String path, final byte[] postBytes, final int socketTimeoutMillis) throws HttpClientException {
        return roundRobin(new HttpCall() {
            @Override
            public HttpResponse call(HttpClient client) throws HttpClientException {
                return client.postBytes(path, postBytes, socketTimeoutMillis);
            }
        });
    }
}
