/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-$year$ Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package com.jivesoftware.os.upena.deployable.profiler.server.endpoints;

import com.jivesoftware.os.upena.deployable.profiler.model.ServicesCallDepthStack;

/**
 *
 */
public class PerfService {

    private final ServicesCallDepthStack callDepthStack;

    public PerfService(ServicesCallDepthStack callDepthStack) {
        this.callDepthStack = callDepthStack;
    }

    public ServicesCallDepthStack getCallDepthStack() {
        return callDepthStack;
    }
}
