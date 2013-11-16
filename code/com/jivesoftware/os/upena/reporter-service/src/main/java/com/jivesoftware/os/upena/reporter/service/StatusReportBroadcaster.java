/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.upena.reporter.service;

import com.jivesoftware.os.jive.utils.logger.LoggerSummary;
import com.jivesoftware.os.upena.reporter.shared.StatusReport;
import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class StatusReportBroadcaster {

    static public interface StatusReportCallback {

        void annouce(StatusReport statusReport) throws Exception;
    }
    private static ScheduledExecutorService newScheduledThreadPool;
    private static final int startupTimestampInSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

    synchronized public static void initialize(
            String instanceKey,
            long annouceEveryNMills,
            StatusReportCallback statusReportCallback) throws SocketException {
        ArrayList<String> ipAddrs = new ArrayList<>();
        ArrayList<String> hostnames = new ArrayList<>();
        scanNics(ipAddrs, hostnames);

        StatusReport statusReport = new StatusReport(
                UUID.randomUUID().toString(),
                new File("." + File.separator).getAbsolutePath(),
                ManagementFactory.getRuntimeMXBean().getVmName(),
                ManagementFactory.getRuntimeMXBean().getVmVendor(),
                ManagementFactory.getRuntimeMXBean().getVmVersion(),
                hostnames,
                ipAddrs,
                instanceKey,
                (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()),
                startupTimestampInSeconds,
                0.0f,
                0.0f,
                LoggerSummary.INSTANCE.errors,
                LoggerSummary.INSTANCE_EXTERNAL_INTERACTIONS.errors);

        newScheduledThreadPool = Executors.newScheduledThreadPool(1);
        newScheduledThreadPool.scheduleWithFixedDelay(
                new StatusReportTask(statusReport, statusReportCallback),
                0,
                annouceEveryNMills,
                TimeUnit.MILLISECONDS);
    }

    synchronized public static void shutdown() {
        if (newScheduledThreadPool != null) {
            newScheduledThreadPool.shutdownNow();
        }
        newScheduledThreadPool = null;
    }

    synchronized public static boolean isTerminated() {
        return newScheduledThreadPool == null || newScheduledThreadPool.isTerminated();
    }

    private static class StatusReportTask implements Runnable {

        private final List<GarbageCollectorMXBean> garbageCollectors;
        private final OperatingSystemMXBean osBean;
        long lastGCTotalTime;
        private final StatusReportCallback statusReportCallback;
        private final StatusReport statusReport;

        public StatusReportTask(StatusReport statusReport,
                StatusReportCallback statusReportCallback) {
            this.statusReportCallback = statusReportCallback;
            this.statusReport = statusReport;

            garbageCollectors = ManagementFactory.getGarbageCollectorMXBeans();
            osBean = ManagementFactory.getOperatingSystemMXBean();
        }

        @Override
        public void run() {
            try {

                long totalTimeInGC = 0;
                for (GarbageCollectorMXBean gc : garbageCollectors) {
                    totalTimeInGC += gc.getCollectionTime();
                }

                statusReport.percentageOfCPUTimeInGC = ((float) (totalTimeInGC - lastGCTotalTime) / (float) lastGCTotalTime);
                lastGCTotalTime = totalTimeInGC;
                statusReport.timestampInSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
                statusReport.load = (float) osBean.getSystemLoadAverage();
                statusReport.internalErrors = LoggerSummary.INSTANCE.errors;
                statusReport.interactionErrors = LoggerSummary.INSTANCE_EXTERNAL_INTERACTIONS.errors;

                statusReportCallback.annouce(statusReport);

            } catch (Exception x) {
                // We use system err to break recursion created by calling LOG.*
                System.err.println("Failed to broadcast to " + statusReportCallback + x);
            }

        }
    }

    private static void scanNics(List<String> ipAddrs, List<String> hostnames) throws SocketException {

        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces != null && networkInterfaces.hasMoreElements()) {
            NetworkInterface ni = networkInterfaces.nextElement();
            Enumeration<InetAddress> ina = ni.getInetAddresses();
            while (ina != null && ina.hasMoreElements()) {
                InetAddress a = ina.nextElement();
                if (a.isAnyLocalAddress()
                        || a.isLoopbackAddress()
                        || a.isLinkLocalAddress()) {
                    continue;
                }
                ipAddrs.add(a.getHostAddress());
                if (a.getHostName() != null && a.getHostName().length() > 0) {
                    if (!a.getHostAddress().equals(a.getHostName())) {
                        hostnames.add(a.getHostName());
                    }
                }
            }
        }
    }
}