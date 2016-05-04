/*
 * Copyright 2016 jonathan.colt.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jivesoftware.os.upena.deployable;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author jonathan.colt
 */
public class UpenaProxy {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final ExecutorService proxyThreads = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("proxy-%d").build());

    private final int localPort;
    private final String remoteHost;
    private final int remotePort;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicLong proxied = new AtomicLong();

    public UpenaProxy(int localPort, String remoteHost, int remotePort) {
        this.localPort = localPort;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    public int getLocalPort() {
        return localPort;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public boolean isRunnig() {
        return running.get();
    }

    public long getProxied() {
        return proxied.get();
    }

    public void start() throws IOException {
        if (running.compareAndSet(false, true)) {
            proxyThreads.submit(() -> {
                try (ServerSocket ss = new ServerSocket(localPort)) {
                    ss.setSoTimeout(1_000);
                    LOG.info("Proxy bound on {} to {}:{}", localPort, remoteHost, remotePort);

                    byte[] request = new byte[1024];
                    byte[] reply = new byte[4096];

                    while (running.get()) {
                        try {
                            Socket client = ss.accept();
                            proxyThreads.submit(() -> {
                                try {
                                    try (InputStream streamFromClient = client.getInputStream()) {
                                        try (OutputStream streamToClient = client.getOutputStream()) {
                                            try (Socket server = new Socket(remoteHost, remotePort)) {
                                                try (InputStream streamFromServer = server.getInputStream()) {

                                                    AtomicBoolean error = new AtomicBoolean();
                                                    OutputStream streamToServer = server.getOutputStream();
                                                    proxyThreads.submit(() -> {
                                                        int bytesRead;
                                                        try {
                                                            while ((bytesRead = streamFromClient.read(request)) != -1) {
                                                                streamToServer.write(request, 0, bytesRead);
                                                                streamToServer.flush();
                                                            }
                                                        } catch (IOException e) {
                                                            error.set(true);
                                                        }

                                                        try {
                                                            streamToServer.close();
                                                        } catch (IOException e) {
                                                            error.set(true);
                                                        }
                                                    });

                                                    int bytesRead;
                                                    while ((bytesRead = streamFromServer.read(reply)) != -1 && !error.get()) {
                                                        streamToClient.write(reply, 0, bytesRead);
                                                        streamToClient.flush();
                                                    }

                                                    proxied.incrementAndGet();
                                                }
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                    LOG.error("Proxy Error.", e);
                                } finally {
                                    try {
                                        if (client != null) {
                                            client.close();
                                        }
                                    } catch (IOException e) {
                                    }
                                }
                            });
                        } catch (SocketTimeoutException x) {
                        }
                    }
                } catch (Exception x) {
                    running.compareAndSet(true, false);
                    LOG.error("Proxy Error.", x);
                }
            });
        }
    }

    public void stop() {
        running.compareAndSet(true, false);
    }

}
