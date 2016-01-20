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
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.commons.io.FileUtils;

public class InstancePath {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final File root;
    private final NameAndKey[] path;

    public InstancePath(File root, NameAndKey[] path) {
        this.root = root;
        this.path = path;
    }

    public String toHumanReadableName() {
        StringBuilder key = new StringBuilder();
        for (NameAndKey p : path) {
            if (key.length() > 0) {
                key.append("/");
            }
            key.append(p.name);
        }
        return key.toString();
    }

    File instanceProperties() {
        File instanceProperties = new File(path(path, -1), "config/instance.properties");
        File parentFile = instanceProperties.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw new RuntimeException("Failed trying to mkdirs for " + parentFile);
        }
        return instanceProperties;
    }

    static String instancePrefix = "InstanceConfig_default_";

    InstanceDescriptor readInstanceDescriptor() throws FileNotFoundException, IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(instanceProperties()));

        System.out.println("readInstanceDescriptor:" + properties);
        Object enabled = properties.get(instancePrefix + "enabled");
        if (enabled == null) {
            enabled = "true";
        }

        Object datacenter = properties.get(instancePrefix + "datacenter");
        Object rack = properties.get(instancePrefix + "rack");
        Object publicHost = properties.get(instancePrefix + "publicHost");
        InstanceDescriptor id = new InstanceDescriptor((datacenter == null) ? "unknownDatacenter" : datacenter.toString(),
            (rack == null) ? "unknownRack" : rack.toString(),
            (publicHost == null) ? "unknown" : publicHost.toString(),
            properties.get(instancePrefix + "clusterKey").toString(),
            properties.get(instancePrefix + "clusterName").toString(),
            properties.get(instancePrefix + "serviceKey").toString(),
            properties.get(instancePrefix + "serviceName").toString(),
            properties.get(instancePrefix + "releaseGroupKey").toString(),
            properties.get(instancePrefix + "releaseGroupName").toString(),
            properties.get(instancePrefix + "instanceKey").toString(),
            Integer.parseInt(properties.get(instancePrefix + "instanceName").toString()),
            properties.get(instancePrefix + "version").toString(),
            properties.get(instancePrefix + "repository").toString(),
            -1,
            Boolean.parseBoolean(enabled.toString()));

        for (Object key : properties.keySet()) {
            String portKey = key.toString();
            if (portKey.endsWith("Port")) {
                if (!portKey.endsWith("routesPort")) { // ignore injected upena
                    String portName = portKey.substring(instancePrefix.length(), portKey.length() - 4);
                    id.ports.put(portName, new InstanceDescriptor.InstanceDescriptorPort(Integer.parseInt(properties.getProperty(key.toString()))));
                }
            }
        }
        LOG.debug("Read instance descriptor:" + id + " from:" + instanceProperties());
        return id;
    }

    void writeInstanceDescriptor(String datacenter, String rack, String publicHostName, String host, String upenaHost, int upenaPort, InstanceDescriptor id)
        throws IOException {
        List<String> properties = new ArrayList<>();
        properties.add(instancePrefix + "datacenter=" + datacenter);
        properties.add(instancePrefix + "rack=" + rack);
        properties.add(instancePrefix + "publicHost=" + publicHostName);
        properties.add(instancePrefix + "host=" + host);
        properties.add(instancePrefix + "routesHost=" + upenaHost); // inject upena
        properties.add(instancePrefix + "routesPort=" + upenaPort);

        properties.add(instancePrefix + "clusterKey=" + id.clusterKey);
        properties.add(instancePrefix + "clusterName=" + id.clusterName);
        properties.add(instancePrefix + "serviceKey=" + id.serviceKey);
        properties.add(instancePrefix + "serviceName=" + id.serviceName);
        properties.add(instancePrefix + "releaseGroupKey=" + id.releaseGroupKey);
        properties.add(instancePrefix + "releaseGroupName=" + id.releaseGroupName);
        properties.add(instancePrefix + "instanceKey=" + id.instanceKey);
        properties.add(instancePrefix + "instanceName=" + id.instanceName);
        properties.add(instancePrefix + "version=" + id.versionName);
        properties.add(instancePrefix + "repository=" + id.repository);
        properties.add(instancePrefix + "enabled=" + String.valueOf(id.enabled));

        for (Entry<String, InstanceDescriptor.InstanceDescriptorPort> port : id.ports.entrySet()) {
            properties.add(instancePrefix + port.getKey() + "Port=" + port.getValue().port);
        }

        FileUtils.writeLines(instanceProperties(), "UTF-8", properties, "\n", false);
    }

    File script(String scriptName) {
        return new File(path(path, -1), "bin/" + scriptName);
    }

    File lib() {
        return new File(path(path, -1), "lib/");
    }

    File pluginLib() {
        return new File(path(path, -1), "plugin-lib/");
    }

    File serviceRoot() {
        return path(path, -1);
    }

    File artifactRoot() {
        return path(path, path.length - 1);
    }

    File artifactFile(String ext) {
        return new File(path(path, path.length - 1), path[path.length - 1].key + ext);
    }

    File path(NameAndKey[] path, int length) {
        if (length == -1 || length > path.length) {
            length = path.length;
        }
        StringBuilder fsPath = new StringBuilder();
        for (int i = 0; i < length; i++) {
            NameAndKey p = path[i];
            if (fsPath.length() > 0) {
                fsPath.append(File.separator);
            }
            fsPath.append(p.toStringForm());
        }
        File folder = new File(root, fsPath.toString());
        if (!folder.exists() && !folder.mkdirs()) {
            throw new RuntimeException("Failed trying to mkdirs for " + folder);
        }
        return folder;
    }

    @Override
    public String toString() {
        return "InstancePath{" + "root=" + root + ", path=" + Arrays.deepToString(path) + '}';
    }
}
