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
package com.jivesoftware.os.upena.amza.service.discovery;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.amza.service.UpenaAmzaService;
import com.jivesoftware.os.upena.amza.shared.UpenaRingHost;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * This is still a work in progress.
 */
public class AmzaDiscovery {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final UpenaAmzaService upenaAmzaService;
    private final String clusterName;
    private final UpenaRingHost ringHost;
    private final InetAddress multicastGroup;
    private final int multicastPort;
    private final ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("discovery-%d").build();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, namedThreadFactory);

    public AmzaDiscovery(UpenaAmzaService upenaAmzaService, UpenaRingHost ringHost, String clusterName, String multicastGroup, int multicastPort) throws UnknownHostException {
        this.upenaAmzaService = upenaAmzaService;
        this.ringHost = ringHost;
        this.clusterName = clusterName;
        this.multicastGroup = InetAddress.getByName(multicastGroup);
        this.multicastPort = multicastPort;
    }

    public void start() throws IOException {
        executor.scheduleWithFixedDelay(new MulticastReceiver(), 0, 10, TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay(new MulticastBroadcaster(), 0, 10, TimeUnit.SECONDS);
    }

    class MulticastReceiver implements Runnable {

        @Override
        public void run() {
            try {
                try (MulticastSocket socket = new MulticastSocket(multicastPort)) {
                    socket.joinGroup(multicastGroup);
                    try {
                        byte[] buf = new byte[512];
                        while (true) {
                            DatagramPacket packet = new DatagramPacket(buf, buf.length);
                            socket.receive(packet);
                            String received = new String(packet.getData(), 0, packet.getLength());
                            String[] clusterHostPort = received.split(":");
                            if (clusterHostPort.length == 3 && clusterHostPort[0] != null && clusterHostPort[0].equals(clusterName)) {
                                LOG.debug("received:" + Arrays.toString(clusterHostPort));
                                String host = clusterHostPort[1];
                                int port = Integer.parseInt(clusterHostPort[2].trim());
                                UpenaRingHost anotherRingHost = new UpenaRingHost(host, port); // TODO need to broadcast rack id
                                List<UpenaRingHost> ring = upenaAmzaService.getRing("master");
                                if (!ring.contains(anotherRingHost)) {
                                    LOG.info("Adding host to the cluster: " + anotherRingHost);
                                    upenaAmzaService.addRingHost("master", anotherRingHost);
                                }
                            }
                        }
                    } catch (Exception x) {
                        LOG.error("Failed to receive broadcast from  group:" + multicastGroup + " port:" + multicastPort, x);
                    } finally {
                        socket.leaveGroup(multicastGroup);
                    }
                }
            } catch (IOException x) {
                LOG.error("Issue with MulticastReceiver", x);
            }
        }
    }

    class MulticastBroadcaster implements Runnable {

        @Override
        public void run() {
            String message = (clusterName + ":" + ringHost.getHost() + ":" + ringHost.getPort());
            try (MulticastSocket socket = new MulticastSocket()) {
                byte[] buf = new byte[512];
                byte[] rawMessage = message.getBytes();
                System.arraycopy(rawMessage, 0, buf, 0, rawMessage.length);
                DatagramPacket packet = new DatagramPacket(buf, buf.length, multicastGroup, multicastPort);
                socket.send(packet);
                LOG.debug("Sent:" + message);
            } catch (IOException e) {
                LOG.error("Failed to receive broadcast. message:" + message + " to  group:" + multicastGroup + " port:" + multicastPort, e);
            }
        }
    }
}
