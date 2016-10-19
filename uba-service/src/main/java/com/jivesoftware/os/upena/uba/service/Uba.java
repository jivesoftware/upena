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

import com.jivesoftware.os.uba.shared.PasswordStore;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptor;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Uba {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final PasswordStore passwordStore;
    final RepositoryProvider repositoryProvider;
    final String datacenter;
    final String rack;
    final String publicHost;
    final String host;
    final String upenaHost;
    final int upenaPort;
    private final UbaTree ubaTree;
    private final DeployableScriptInvoker invokeScript;
    private final UbaLog ubaLog;
    private final Cache<String, Boolean> haveRunConfigExtractionCache;

    public Uba(PasswordStore passwordStore,
        RepositoryProvider repositoryProvider,
        String datacenter,
        String rack,
        String publicHostName,
        String host,
        String upenaHost,
        int upenaPort,
        UbaTree ubaTree,
        UbaLog ubaLog) {

        this.passwordStore = passwordStore;
        this.repositoryProvider = repositoryProvider;
        this.datacenter = datacenter;
        this.rack = rack;
        this.publicHost = publicHostName;
        this.host = host;
        this.upenaHost = upenaHost;
        this.upenaPort = upenaPort;
        this.ubaTree = ubaTree;
        this.invokeScript = new DeployableScriptInvoker(Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicLong count = new AtomicLong();

            @Override
            public Thread newThread(Runnable r) {
                long id = count.incrementAndGet();
                return new Thread(r, "Script onvoker thread-" + id);
            }
        }));
        this.ubaLog = ubaLog;
        this.haveRunConfigExtractionCache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.DAYS).build();
    }

    public Collection<InstancePathAndDescriptor> getOnDiskInstances() {
        Map<String, InstancePathAndDescriptor> instances = new HashMap<>();
        ubaTree.build((NameAndKey[] path) -> {
            InstancePath instancePath = new InstancePath(ubaTree.getRoot(), path);
            try {
                if (instancePath.instanceProperties().exists()) {
                    InstanceDescriptor id = instancePath.readInstanceDescriptor();
                    InstancePathAndDescriptor instancePathAndDescriptor = new InstancePathAndDescriptor(instancePath, id);
                    InstancePathAndDescriptor had = instances.put(id.instanceKey, instancePathAndDescriptor);
                    if (had != null) {
                        LOG.warn("{} collides with {} news one wins. Need to figure out why this is happening.", had, instancePathAndDescriptor);
                    }
                }
            } catch (IOException ex) {
                LOG.error("Failed build instances from disk.", ex);
            }
        });
        return instances.values();
    }

    public static class InstancePathAndDescriptor {

        public final InstancePath path;
        public final InstanceDescriptor descriptor;

        public InstancePathAndDescriptor(InstancePath instancePath, InstanceDescriptor instanceDescriptor) {
            this.path = instancePath;
            this.descriptor = instanceDescriptor;
        }

        @Override
        public String toString() {
            return "InstancePathAndDescriptor{" + "instancePath=" + path + ", instanceDescriptor=" + descriptor + '}';
        }

    }

    public String instanceDescriptorKey(InstanceDescriptor instanceDescriptor, InstancePath instancePath) {
        StringBuilder key = new StringBuilder();
        key.append(instanceDescriptor.clusterKey).append('|');
        key.append(instanceDescriptor.serviceKey).append('|');
        key.append(instanceDescriptor.releaseGroupKey).append('|');
        key.append(instanceDescriptor.instanceKey).append('-');
        key.append(instancePath.toHumanReadableName());
        return key.toString();
    }

    InstancePath instancePath(InstanceDescriptor instanceDescriptor) {
        return new InstancePath(ubaTree.getRoot(), new NameAndKey[]{
            new NameAndKey(instanceDescriptor.clusterName, instanceDescriptor.clusterKey),
            new NameAndKey(instanceDescriptor.serviceName, instanceDescriptor.serviceKey),
            new NameAndKey(instanceDescriptor.releaseGroupName, instanceDescriptor.releaseGroupKey),
            new NameAndKey(Integer.toString(instanceDescriptor.instanceName), instanceDescriptor.instanceKey)
        });
    }

    Nanny newNanny(InstanceDescriptor instanceDescriptor, InstancePath instancePath) {
        DeployLog deployLog = new DeployLog();
        HealthLog healthLog = new HealthLog(deployLog);
        return new Nanny(passwordStore,
            repositoryProvider,
            instanceDescriptor,
            instancePath,
            new DeployableValidator(),
            new DeployLog(),
            healthLog,
            invokeScript,
            ubaLog,
            haveRunConfigExtractionCache);
    }

}
