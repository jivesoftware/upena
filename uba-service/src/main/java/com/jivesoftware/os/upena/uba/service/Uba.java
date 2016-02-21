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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptor;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Uba {

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
    private final Cache<InstanceDescriptor, Boolean> haveRunConfigExtractionCache;

    public Uba(RepositoryProvider repositoryProvider,
        String datacenter,
        String rack,
        String publicHostName,
        String host,
        String upenaHost,
        int upenaPort,
        UbaTree ubaTree,
        UbaLog ubaLog) {

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

    public Map<InstanceDescriptor, InstancePath> getOnDiskInstances() {
        final Map<InstanceDescriptor, InstancePath> instances = new ConcurrentHashMap<>();
        ubaTree.build((NameAndKey[] path) -> {
            InstancePath instancePath = new InstancePath(ubaTree.getRoot(), path);
            try {
                if (instancePath.instanceProperties().exists()) {
                    InstanceDescriptor id = instancePath.readInstanceDescriptor();
                    instances.put(id, instancePath);
                }
            } catch (IOException ex) {
                ex.printStackTrace(); //hmmm
            }
        });
        return instances;
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
        return new Nanny(repositoryProvider,
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
