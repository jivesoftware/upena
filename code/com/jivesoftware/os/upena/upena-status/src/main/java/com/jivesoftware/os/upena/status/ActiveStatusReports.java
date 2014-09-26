/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.upena.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.jive.utils.logger.MetricLogger;
import com.jivesoftware.os.jive.utils.logger.MetricLoggerFactory;
import com.jivesoftware.os.upena.reporter.shared.StatusReport;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ActiveStatusReports {

    private static final MetricLogger log = MetricLoggerFactory.getLogger();
    private final File f = new File("./clusterAlerts.json");
    private final ConcurrentHashMap<StatusReport, TimestampAnnouncementPair> statusReports = new ConcurrentHashMap<>();
    private final int keepLastNAlerts;
    private final Alerts alerts;

    public ActiveStatusReports(
            final long serviceConsiderOfflineIfHasntAnnoucenedItselfInNMillis,
            int keepLastNAlerts) throws IOException {
        this.keepLastNAlerts = keepLastNAlerts;
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                removeServiceAnnouncement(serviceConsiderOfflineIfHasntAnnoucenedItselfInNMillis);
            }
        }, 0, 10, TimeUnit.SECONDS);

        if (f.exists()) {
            alerts = new ObjectMapper().readValue(f, Alerts.class);
        } else {
            alerts = new Alerts();
        }
    }

    private void removeServiceAnnouncement(long olderThanNMillis) {
        long timestamp = System.currentTimeMillis();
        for (Map.Entry<StatusReport, TimestampAnnouncementPair> entry : statusReports.entrySet()) {
            if (entry.getValue().timestamp < timestamp - olderThanNMillis) {
                addAlert(new Alert(UUID.randomUUID().toString(), AlertType.OFFLINE, "", (int) TimeUnit.MILLISECONDS.toSeconds(timestamp),
                        entry.getValue().serviceAnnouncement));
                statusReports.remove(entry.getKey());
            }
        }
    }

    public void add(StatusReport serviceAnnouncement) {
        long timestamp = System.currentTimeMillis();
        TimestampAnnouncementPair current = statusReports.get(serviceAnnouncement);
        if (current == null) {
            addAlert(new Alert(UUID.randomUUID().toString(), AlertType.ONLINE, new Date().toString(), (int) TimeUnit.MILLISECONDS.toSeconds(timestamp),
                    serviceAnnouncement));
            statusReports.put(serviceAnnouncement, new TimestampAnnouncementPair(serviceAnnouncement, timestamp));

        } else {
            if (changed(current.serviceAnnouncement, serviceAnnouncement, true, false)) {
                addAlert(new Alert(UUID.randomUUID().toString(), AlertType.RESTARTED, new Date().toString(), (int) TimeUnit.MILLISECONDS.toSeconds(timestamp),
                        serviceAnnouncement));
            }
            if (changed(current.serviceAnnouncement, serviceAnnouncement, false, true)) {
                addAlert(new Alert(UUID.randomUUID().toString(), AlertType.INSTANCE_CHANGED, new Date().toString(),
                        (int) TimeUnit.MILLISECONDS.toSeconds(timestamp), serviceAnnouncement));
            }

            statusReports.put(serviceAnnouncement, new TimestampAnnouncementPair(serviceAnnouncement, timestamp));
        }
    }

    public Set<StatusReport> active() {
        Set<StatusReport> active = new HashSet<>();
        for (Entry<StatusReport, TimestampAnnouncementPair> entry : statusReports.entrySet()) {
            active.add(entry.getValue().serviceAnnouncement);
        }
        return active;
    }

    public List<Alert> listAlerts() {
        return alerts.alerts;
    }

    public void addAlert(Alert alert) {
        if (alert == null) {
            return;
        }
        while (alerts.alerts.size() > keepLastNAlerts) {
            alerts.alerts.remove(0);
        }
        alerts.alerts.add(alert);
        log.info(alert.toString());
        try {
            new ObjectMapper().writeValue(f, alerts);
        } catch (IOException ex) {
            log.error("failed to persist alerts to json file:" + f, ex);
        }
    }

    class DeploymentEnvironment {

        public Map<String, String> deploymentEnvironment = new HashMap<>();
    }

    class TimestampAnnouncementPair {

        StatusReport serviceAnnouncement;
        long timestamp;

        public TimestampAnnouncementPair(StatusReport serviceAnnouncement, long timestamp) {
            this.serviceAnnouncement = serviceAnnouncement;
            this.timestamp = timestamp;
        }
    }

    public boolean changed(StatusReport current, StatusReport next,
            boolean jvmIdCheck,
            boolean versionCheck) {

        if (current.jvmIpAddrs != next.jvmIpAddrs && (current.jvmIpAddrs == null || !current.jvmIpAddrs.equals(next.jvmIpAddrs))) {
            return true;
        }

        if (jvmIdCheck && !current.jvmUID.equals(next.jvmUID)) {
            return true;
        }

        return false;
    }
}
