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
package com.jivesoftware.os.upena.uba.service;

import com.jivesoftware.os.jive.utils.logger.MetricLogger;
import com.jivesoftware.os.jive.utils.logger.MetricLoggerFactory;
import com.jivesoftware.os.uba.shared.UbaReport;
import com.jivesoftware.os.upena.routing.shared.InstanceChanged;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptor;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptorsRequest;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptorsResponse;
import com.jivesoftware.os.upena.routing.shared.TenantChanged;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UbaService {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final UpenaClient upenaClient;
    private final Uba uba;
    private final String hostKey;
    private final Map<String, Nanny> nannies = new ConcurrentHashMap<>();

    public UbaService(
            UpenaClient upenaClient,
            Uba uba,
            String hostKey) {
        this.upenaClient = upenaClient;
        this.uba = uba;
        this.hostKey = hostKey;
    }

    public void instanceChanged(List<InstanceChanged> instanceChanges) throws Exception {
        boolean runNanny = false;
        for (InstanceChanged instanceChanged : instanceChanges) {
            if (hostKey.equals(instanceChanged.getHostKey())) {
                runNanny = true;
            }
        }
        if (runNanny) {
            LOG.info("Nanny triggered by instance declaration changes.");
            nanny();
        }
    }

    public void tenantChanged(TenantChanged tenantChanged) throws Exception {
        for (Nanny nanny : rollCall().values()) {
            nanny.invalidateRouting(tenantChanged.getTenantId());
        }
    }

    synchronized private Map<String, Nanny> rollCall() throws Exception {

        InstanceDescriptorsRequest request = new InstanceDescriptorsRequest(hostKey);
        InstanceDescriptorsResponse declaredInstances = upenaClient.instanceDescriptor(request);
        if (declaredInstances.decommisionRequestingHost) {
            decommisionOrchestraService();
            return Collections.emptyMap();
        } else {
            Map<InstanceDescriptor, InstancePath> onDiskInstances = uba.getOnDiskInstances();
            for (Map.Entry<InstanceDescriptor, InstancePath> entry : onDiskInstances.entrySet()) {
                InstanceDescriptor instanceDescriptor = entry.getKey();
                String nanneyKey = uba.instanceDescriptorKey(instanceDescriptor);
                if (!nannies.containsKey(nanneyKey)) {
                    nannies.put(nanneyKey, uba.newNanny(instanceDescriptor));
                }
            }

            Set<String> expectedPlayer = new HashSet<>();
            List<Nanny> newPlayers = new ArrayList<>();

            for (InstanceDescriptor instanceDescriptor : declaredInstances.instanceDescriptors) {
                String nannyKey = uba.instanceDescriptorKey(instanceDescriptor);
                Nanny currentNanny = nannies.get(nannyKey);
                if (currentNanny == null) {
                    LOG.debug("New nanny:" + nannyKey);
                    Nanny nanny = uba.newNanny(instanceDescriptor);
                    newPlayers.add(nanny);
                } else {
                    LOG.debug("Existing nanny:" + nannyKey);
                    currentNanny.setInstanceDescriptor(instanceDescriptor);
                    expectedPlayer.add(uba.instanceDescriptorKey(currentNanny.getInstanceDescriptor()));
                }
            }

            Set<String> unexpectedPlayerKeys = new HashSet<>(nannies.keySet());
            unexpectedPlayerKeys.removeAll(expectedPlayer);
            if (!unexpectedPlayerKeys.isEmpty()) {
                for (String unexpectedPlayerKey : unexpectedPlayerKeys) {
                    Nanny nanny = nannies.get(unexpectedPlayerKey);
                    LOG.info("Destroying player:" + nanny);
                    nanny.destroy();
                    nanny.stop();
                    nannies.remove(unexpectedPlayerKey);
                }
            }

            for (Nanny newPlayer : newPlayers) {
                LOG.info("Deploying newPlayer:" + newPlayer);
                nannies.put(uba.instanceDescriptorKey(newPlayer.getInstanceDescriptor()), newPlayer);
            }
            return nannies;
        }
    }

    public void decommisionOrchestraService() throws Exception {
        for (Nanny nanny : rollCall().values()) {
            nanny.destroy();
        }
    }

    public List<String> nanny() throws Exception {
        for (Nanny nanny : rollCall().values()) {
            nanny.nanny(uba.host, uba.upenaHost, uba.upenaPort);
        }
        return new ArrayList<>();
    }

    public UbaReport report() throws Exception {
        UbaReport report = new UbaReport();
        for (Nanny nanny : rollCall().values()) {
            nanny.nanny(uba.host, uba.upenaHost, uba.upenaPort);
            report.nannyReports.add(nanny.report());
        }
        return report;
    }
}
