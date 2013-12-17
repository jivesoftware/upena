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

import com.jivesoftware.os.upena.routing.shared.InstanceDescriptor;
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

    private final File root;
    private final NameAndKey[] path;
    private String host;
    private String upenaHost;
    private int upenaPort;

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
        instanceProperties.getParentFile().mkdirs();
        return instanceProperties;
    }

    static String instancePrefix = "InstanceConfig_default_";

    InstanceDescriptor readInstanceDescriptor() throws FileNotFoundException, IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(instanceProperties()));
        host = properties.get(instancePrefix + "host").toString();
        upenaHost = properties.get(instancePrefix + "routesHost").toString();
        upenaPort = Integer.parseInt(properties.get(instancePrefix + "routesPort").toString());

        System.out.println("readInstanceDescriptor:"+properties);

        InstanceDescriptor id = new InstanceDescriptor(properties.get(instancePrefix + "clusterKey").toString(),
                properties.get(instancePrefix + "clusterName").toString(),
                properties.get(instancePrefix + "serviceKey").toString(),
                properties.get(instancePrefix + "serviceName").toString(),
                properties.get(instancePrefix + "releaseGroupKey").toString(),
                properties.get(instancePrefix + "releaseGroupName").toString(),
                properties.get(instancePrefix + "instanceKey").toString(),
                Integer.parseInt(properties.get(instancePrefix + "instanceName").toString()),
                properties.get(instancePrefix + "version").toString(),
                properties.get(instancePrefix + "repository").toString());

        for (Object key : properties.keySet()) {
            String portKey = key.toString();
            if (portKey.endsWith("Port")) {
                String portName = portKey.substring(instancePrefix.length(), portKey.length() - 4);
                id.ports.put(portName, new InstanceDescriptor.InstanceDescriptorPort(Integer.parseInt(properties.getProperty(key.toString()))));
            }
        }
        return id;
    }

    void writeInstanceDescriptor(String host, String upenaHost, int upenaPort, InstanceDescriptor id) throws IOException {
        this.host = host;
        this.upenaHost = upenaHost;
        this.upenaPort = upenaPort;
        List<String> properties = new ArrayList<>();
        properties.add(instancePrefix + "host=" + host);
        properties.add(instancePrefix + "routesHost=" + upenaHost);
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
        folder.mkdirs();
        return folder;
    }

    @Override
    public String toString() {
        return "InstancePath{" + "root=" + root + ", path=" + Arrays.deepToString(path) + '}';
    }
}
