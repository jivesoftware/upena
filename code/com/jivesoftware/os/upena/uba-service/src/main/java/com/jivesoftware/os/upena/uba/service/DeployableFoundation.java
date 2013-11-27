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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;

public class DeployableFoundation {

    private final ObjectMapper mapper;

    public DeployableFoundation(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void initialize(String host, String upenaHost, int upenaPort, InstanceDescriptor id, InstancePath instancePath, DeployLog deployLog) {
        File instanceDescriptorFile = new File(instancePath.serviceRoot(), "instanceDescriptor.json");
        try {
            mapper.writeValue(instanceDescriptorFile, id);
            deployLog.log("wrote overlay instanceDescriptor.json.", null);
        } catch (IOException x) {
            deployLog.log("error while writing overlay instanceDescriptor.json.", x);
        }

        File overlayProperties = new File(instancePath.serviceRoot(), "overlay-config.properties");

        List<String> overlay = new ArrayList<>();
        overlay.add("RoutableDeployableConfig/default/instanceGUID=" + id.instanceKey);
        overlay.add("RoutableDeployableConfig/default/cluster=" + id.clusterName);
        overlay.add("RoutableDeployableConfig/default/host=" + host);
        overlay.add("RoutableDeployableConfig/default/serviceName=" + id.instanceKey);
        overlay.add("RoutableDeployableConfig/default/version=" + id.versionName);
        overlay.add("RoutableDeployableConfig/default/instanceId=" + id.instanceName);
        overlay.add("RoutableDeployableConfig/default/managePort=" + id.ports.get("manage").port);
        overlay.add("RoutableDeployableConfig/default/port=" + id.ports.get("main").port);
        overlay.add("RoutableDeployableConfig/default/routesHost=" + upenaHost);
        overlay.add("RoutableDeployableConfig/default/routesPort=" + upenaPort);

        try {
            FileUtils.writeLines(overlayProperties, "UTF-8", overlay, "\n", false);
            deployLog.log("wrote overlay properties.", null);
        } catch (IOException x) {
            deployLog.log("error while writing overlay properties.", x);
        }

        File instanceEnvFile = new File(instancePath.serviceRoot(), "instance-env.sh");

        List<String> instanceEnv = new ArrayList<>();
        instanceEnv.add("#!/bin/bash");
        instanceEnv.add("JAVA_XMS=64");
        instanceEnv.add("JAVA_XMX=512");
        instanceEnv.add("UPENA_HOST=" + upenaHost);
        instanceEnv.add("UPENA_PORT=" + upenaPort);
        instanceEnv.add("CLUSTER_KEY=" + id.clusterKey);
        instanceEnv.add("SERVICE_KEY=" + id.serviceKey);
        instanceEnv.add("RELEASE_GROUP_KEY=" + id.releaseGroupKey);
        instanceEnv.add("INSTANCE_KEY=" + id.instanceKey);

        for (Map.Entry<String, InstanceDescriptor.InstanceDescriptorPort> p : id.ports.entrySet()) {
            instanceEnv.add(p.getKey().toUpperCase() + "_PORT" + "=" + p.getValue().port);
        }

        try {
            FileUtils.writeLines(instanceEnvFile, "UTF-8", instanceEnv, "\n", false);
            deployLog.log("wrote instance-env.sh.", null);
        } catch (IOException x) {
            deployLog.log("error while writing overlay instance-env.sh.", x);
        }
    }
}
