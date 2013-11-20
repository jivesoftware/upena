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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.upena.routing.shared.InstanceDescriptor;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Uba {

    final String host;
    final String upenaHost;
    final int upenaPort;
    private final ObjectMapper mapper;
    private final UbaTree ubaTree;

    public Uba(String host, String upenaHost, int upenaPort, ObjectMapper mapper, UbaTree ubaTree) {
        this.host = host;
        this.upenaHost = upenaHost;
        this.upenaPort = upenaPort;
        this.mapper = mapper;
        this.ubaTree = ubaTree;
    }

    public Map<InstanceDescriptor, InstancePath> getOnDiskInstances() {
        final Map<InstanceDescriptor, InstancePath> instances = new ConcurrentHashMap<>();
        ubaTree.build(new UbaTree.ConductorPathCallback() {
            @Override
            public void conductorPath(NameAndKey[] path) {
                InstancePath instancePath = new InstancePath(ubaTree.getRoot(), path);
                InstanceDescriptor instanceDescriptor = instanceDescriptor(instancePath);
                if (instanceDescriptor != null) {
                    instances.put(instanceDescriptor, instancePath);
                }
            }
        });
        return instances;
    }

    public String instanceDescriptorKey(InstanceDescriptor instanceDescriptor) {
        StringBuilder key = new StringBuilder();
        key.append(instanceDescriptor.clusterKey).append('|');
        key.append(instanceDescriptor.serviceKey).append('|');
        key.append(instanceDescriptor.releaseGroupKey).append('|');
        key.append(instanceDescriptor.instanceKey);
        return key.toString();
    }

    Nanny newNanny(InstanceDescriptor instanceDescriptor) {
        InstancePath instancePath = new InstancePath(ubaTree.getRoot(), new NameAndKey[]{
            new NameAndKey(instanceDescriptor.clusterName, instanceDescriptor.clusterKey),
            new NameAndKey(instanceDescriptor.serviceName, instanceDescriptor.serviceKey),
            new NameAndKey(instanceDescriptor.releaseGroupName, instanceDescriptor.releaseGroupKey),
            new NameAndKey(Integer.toString(instanceDescriptor.instanceName), instanceDescriptor.instanceKey)
        });
        return new Nanny(mapper, instanceDescriptor, instancePath);
    }

    private InstanceDescriptor instanceDescriptor(InstancePath instancePath) {
        File instanceDescriptorFile = new File(instancePath.serviceRoot(), "instanceDescriptor.json");
        if (instanceDescriptorFile.exists()) {
            try {
                return mapper.readValue(instanceDescriptorFile, InstanceDescriptor.class);
            } catch (Exception x) {
                x.printStackTrace(); // Hmmm
                return null;
            }
        } else {
            return null;
        }
    }

}