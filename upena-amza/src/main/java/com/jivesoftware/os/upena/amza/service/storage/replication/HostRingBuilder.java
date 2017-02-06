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
package com.jivesoftware.os.upena.amza.service.storage.replication;

import com.jivesoftware.os.upena.amza.shared.UpenaRingHost;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.util.ArrayList;
import java.util.Collection;

public class HostRingBuilder {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    public HostRing build(UpenaRingHost serviceHost, Collection<UpenaRingHost> ringHosts) {
        ArrayList<UpenaRingHost> ring = new ArrayList<>(ringHosts);
        int rootIndex = -1;
        int index = 0;
        for (UpenaRingHost host : ring) {
            if (host.equals(serviceHost)) {
                rootIndex = index;
                break;
            }
            index++;
        }
        if (rootIndex == -1) {
            LOG.warn("serviceHost: " + serviceHost + " is not a member of the ring.");
            return new HostRing(new UpenaRingHost[0], new UpenaRingHost[0]);
        }

        ArrayList<UpenaRingHost> above = new ArrayList<>();
        ArrayList<UpenaRingHost> below = new ArrayList<>();
        int aboveI = rootIndex - 1;
        int belowI = rootIndex + 1;
        for (int i = 1; i < ring.size(); i++) {
            if (aboveI < 0) {
                aboveI = ring.size() - 1;
            }
            if (belowI >= ring.size()) {
                belowI = 0;
            }
            above.add(ring.get(aboveI));
            below.add(ring.get(belowI));
            aboveI--;
            belowI++;
        }
        return new HostRing(above.toArray(new UpenaRingHost[above.size()]), below.toArray(new UpenaRingHost[below.size()]));
    }
}
