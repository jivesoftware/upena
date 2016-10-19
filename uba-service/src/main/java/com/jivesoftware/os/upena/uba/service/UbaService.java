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

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.InstanceChanged;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptor;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptorsRequest;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptorsResponse;
import com.jivesoftware.os.routing.bird.shared.TenantChanged;
import com.jivesoftware.os.uba.shared.PasswordStore;
import com.jivesoftware.os.uba.shared.UbaReport;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.FileUtils;

public class UbaService {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final PasswordStore passwordStore;
    private final UpenaClient upenaClient;
    private final Uba uba;
    private final String hostKey;
    private final Map<String, Nanny> nannies = new ConcurrentHashMap<>();

    public UbaService(
        PasswordStore passwordStore,
        UpenaClient upenaClient,
        Uba uba,
        String hostKey) {

        this.passwordStore = passwordStore;
        this.upenaClient = upenaClient;
        this.uba = uba;
        this.hostKey = hostKey;
    }

    public Iterable<Entry<String, Nanny>> iterateNannies() {
        return nannies.entrySet();
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
            decommisionUbaService();
            return Collections.emptyMap();
        } else {
            Collection<Uba.InstancePathAndDescriptor> onDiskInstances = uba.getOnDiskInstances();
            for (Uba.InstancePathAndDescriptor instance : onDiskInstances) {
                InstanceDescriptor instanceDescriptor = instance.descriptor;
                InstancePath instancePath = instance.path;

                String nanneyKey = uba.instanceDescriptorKey(instanceDescriptor, instancePath);
                if (!nannies.containsKey(nanneyKey)) {
                    if (ensureCerts(instancePath, instanceDescriptor)) {
                        nannies.put(nanneyKey, uba.newNanny(instanceDescriptor, instancePath));
                    }
                }
            }

            Set<String> expectedPlayer = new HashSet<>();
            List<Nanny> newPlayers = new ArrayList<>();

            for (InstanceDescriptor instanceDescriptor : declaredInstances.instanceDescriptors) {
                InstancePath path = uba.instancePath(instanceDescriptor);
                String nannyKey = uba.instanceDescriptorKey(instanceDescriptor, path);
                Nanny currentNanny = nannies.get(nannyKey);
                if (currentNanny == null) {
                    LOG.debug("New nanny:" + nannyKey);
                    if (ensureCerts(path, instanceDescriptor)) {
                        Nanny nanny = uba.newNanny(instanceDescriptor, path);
                        newPlayers.add(nanny);
                    }
                } else {
                    LOG.debug("Existing nanny:" + nannyKey);
                    if (ensureCerts(path, instanceDescriptor)) {
                        currentNanny.setInstanceDescriptor(instanceDescriptor);
                    }
                    expectedPlayer.add(uba.instanceDescriptorKey(currentNanny.getInstanceDescriptor(), path));
                }
            }

            Set<String> unexpectedPlayerKeys = new HashSet<>(nannies.keySet());
            unexpectedPlayerKeys.removeAll(expectedPlayer);
            if (!unexpectedPlayerKeys.isEmpty()) {
                for (String unexpectedPlayerKey : unexpectedPlayerKeys) {
                    Nanny nanny = nannies.get(unexpectedPlayerKey);
                    LOG.info("Destroying service:" + nanny);
                    try {
                        nanny.destroy();
                        nanny.stop();
                        nannies.remove(unexpectedPlayerKey);
                    } catch (InterruptedException | ExecutionException x) {
                        LOG.error("Failed to destroy " + nanny.getInstanceDescriptor(), x);
                    }
                }
            }

            for (Nanny newPlayer : newPlayers) {
                LOG.info("Deploying newPlayer:" + newPlayer);
                InstancePath path = uba.instancePath(newPlayer.getInstanceDescriptor());
                nannies.put(uba.instanceDescriptorKey(newPlayer.getInstanceDescriptor(), path), newPlayer);
            }
            return nannies;
        }
    }

    private boolean ensureCerts(InstancePath instancePath, InstanceDescriptor id) throws Exception {
        boolean sslEnabled = false;
        for (Map.Entry<String, InstanceDescriptor.InstanceDescriptorPort> entry : id.ports.entrySet()) {
            if (entry.getValue().sslEnabled) {
                sslEnabled = true;
                break;
            }
        }
        if (sslEnabled) {
            SelfSigningCertGenerator generator = new SelfSigningCertGenerator();
            String password = passwordStore.password(id.instanceKey + "-ssl");
            File certFile = instancePath.certs("sslKeystore");
            if (!certFile.exists() || !generator.validate(id.instanceKey, password, certFile)) {
                FileUtils.deleteQuietly(certFile);
                generator.create(id.instanceKey, password, certFile);
            }
        }

        File oauthKeystoreFile = instancePath.certs("oauthKeystore");
        File oauthPublicKeyFile = instancePath.certs("oauthPublicKey");
        String password = passwordStore.password(id.instanceKey + "-oauth");
        RSAKeyPairGenerator generator = new RSAKeyPairGenerator();
        if (id.publicKey == null || !id.publicKey.equals(generator.getPublicKey(id.instanceKey, password, oauthKeystoreFile, oauthPublicKeyFile))) {
            FileUtils.deleteQuietly(oauthKeystoreFile);
            FileUtils.deleteQuietly(oauthPublicKeyFile);
            generator.create(id.instanceKey, password, oauthKeystoreFile, oauthPublicKeyFile);
            upenaClient.updateKeyPair(id.instanceKey, generator.getPublicKey(id.instanceKey, password, oauthKeystoreFile, oauthPublicKeyFile));
            return false;
        }
        return true;
    }

    public void decommisionUbaService() throws Exception {
        for (Nanny nanny : rollCall().values()) {
            try {
                nanny.destroy();
            } catch (InterruptedException | ExecutionException x) {
                LOG.error("Failed to destroy " + nanny.getInstanceDescriptor(), x);
            }
        }
    }

    public List<String> nanny() throws Exception {
        for (Nanny nanny : rollCall().values()) {
            nanny.nanny(uba.datacenter, uba.rack, uba.publicHost, uba.host, uba.upenaHost, uba.upenaPort);
        }
        return new ArrayList<>();
    }

    public UbaReport report() throws Exception {
        UbaReport report = new UbaReport();
        for (Nanny nanny : rollCall().values()) {
            report.nannyReports.add(nanny.report());
            nanny.nanny(uba.datacenter, uba.rack, uba.publicHost, uba.host, uba.upenaHost, uba.upenaPort);
        }
        return report;
    }
}
