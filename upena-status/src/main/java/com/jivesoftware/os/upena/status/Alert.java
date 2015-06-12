/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.upena.status;

import com.jivesoftware.os.routing.bird.deployable.reporter.shared.StatusReport;

/**
 *
 */
public class Alert {

    public String id;
    public AlertType type;
    public String message;
    public int timestampInSeconds;
    public StatusReport statusReport;

    public Alert() {
    }

    public Alert(String id, AlertType type, String message, int timestampInSeconds, StatusReport statusReport) {
        this.id = id;
        this.type = type;
        this.message = message;
        this.timestampInSeconds = timestampInSeconds;
        this.statusReport = statusReport;
    }

    @Override
    public String toString() {
        return "Alert{" + "id=" + id + ", type=" + type + ", message=" + message + ", timestamp=" + timestampInSeconds + ", serviceAnnouncement="
            + statusReport + '}';
    }
}
