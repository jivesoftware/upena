/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.upena.reporter.shared;

import java.util.List;

/**
 * Deliberately not final and not a formal bean because its expected that jackson will be used to map to and from json.
 */
public class StatusReport {

    public String jvmUID;
    public String jvmHome;
    public String jvmName;
    public String jvmVender;
    public String jvmVersion;
    public List<String> jvmHostnames;
    public List<String> jvmIpAddrs;
    public String instanceKey;
    public int timestampInSeconds;
    public int startupTimestampInSeconds;
    public float load;
    public float percentageOfCPUTimeInGC;
    public long internalErrors = 0;
    public long interactionErrors = 0;

    public StatusReport() {
    }

    public StatusReport(String jvmUID,
            String jvmHome,
            String jvmName,
            String jvmVender,
            String jvmVersion,
            List<String> jvmHostnames,
            List<String> jvmIpAddrs,
            String instanceKey,
            int timestampInSeconds,
            int startupTimestampInSeconds,
            float load,
            float percentageOfCPUTimeInGC,
            long internalErrors,
            long interactionErrors) {

        this.jvmUID = jvmUID;
        this.jvmHome = jvmHome;
        this.jvmName = jvmName;
        this.jvmVender = jvmVender;
        this.jvmVersion = jvmVersion;
        this.jvmHostnames = jvmHostnames;
        this.jvmIpAddrs = jvmIpAddrs;
        this.instanceKey = instanceKey;
        this.timestampInSeconds = timestampInSeconds;
        this.startupTimestampInSeconds = startupTimestampInSeconds;
        this.load = load;
        this.percentageOfCPUTimeInGC = percentageOfCPUTimeInGC;
        this.internalErrors = internalErrors;
        this.interactionErrors = interactionErrors;
    }

}
