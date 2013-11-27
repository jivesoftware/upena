/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.upena.status;

import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class Alerts {

    public int timestampInSeconds;
    public List<Alert> alerts = new LinkedList<>();

    public Alerts() {
    }

    public Alerts(int timestampInSeconds, List<Alert> alerts) {
        this.timestampInSeconds = timestampInSeconds;
        this.alerts = alerts;
    }
}
