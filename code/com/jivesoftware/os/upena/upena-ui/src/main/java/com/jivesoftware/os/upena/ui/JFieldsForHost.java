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
package com.jivesoftware.os.upena.ui;

import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.Host;
import com.jivesoftware.os.upena.shared.HostFilter;
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class JFieldsForHost implements JObjectFields<HostKey, Host, HostFilter> {

    JEditKeyField hostKey;
    JEditField name;
    JEditField hostName;
    JEditField port;
    JEditField workingDir;
    JEditRef clusterKey;
    Map<String, JField> fields = new LinkedHashMap<>();
    JObjectFactory factory;
    String key;
    Host value;

    public JFieldsForHost(JObjectFactory factory, String k, Host v) {
        this.factory = factory;
        this.key = k;
        this.value = v;
        name = new JEditField("name", (v != null) ? v.name : "");
        fields.put("name", name);
        hostName = new JEditField("hostName", (v != null) ? v.hostName : "");
        fields.put("hostName", hostName);
        port = new JEditField("port", (v != null) ? Integer.toString(v.port) : "");
        fields.put("port", port);
        workingDir = new JEditField("workingDirectory", (v != null) ? v.workingDirectory : "");
        fields.put("workingDirectory", workingDir);
        clusterKey = new JEditRef(factory, "clusterKey", Cluster.class, (v != null) ? (v.clusterKey != null) ? v.clusterKey.getKey() : "" : "");
        fields.put("clusterKey",
                clusterKey);
        hostKey = new JEditKeyField("key", (k != null) ? k : "");
        fields.put("key", hostKey);

    }

    @Override
    public JObjectFields<HostKey, Host, HostFilter> copy() {
        return new JFieldsForHost(factory, key, value);
    }

    @Override
    public String shortName(Host v) {
        return v.name;
    }

    @Override
    public Host fieldsToObject() {
        Host host = new Host(name.getValue(),
                hostName.getValue(),
                Integer.parseInt(port.getValue()),
                workingDir.getValue(),
                new ClusterKey(clusterKey.getValue()));
        return host;
    }

    @Override
    public Map<String, JField> objectFields() {
        return fields;
    }

    @Override
    public HostKey key() {
        return new HostKey(hostKey.getValue());
    }

    @Override
    public HostFilter fieldsToFilter() {
        HostFilter filter = new HostFilter(name.getValue(),
                hostName.getValue(),
                ((port.getValue().length() == 0) ? null : Integer.parseInt(port.getValue())),
                workingDir.getValue(),
                FilterUtils.nullIfEmpty(new ClusterKey(clusterKey.getValue())),
                0, 100);
        return filter;
    }

    @Override
    public void update(HostKey key, Host v) {
        hostKey.setValue(key.getKey());
        name.setValue(v.name);
        hostName.setValue(v.hostName);
        port.setValue(Integer.toString(v.port));
        workingDir.setValue(v.workingDirectory);
        clusterKey.setValue(v.clusterKey == null ? "" : v.clusterKey.getKey());
    }

    @Override
    public void updateFilter(HostKey key, Host v) {
        name.setValue(v.name);
        hostName.setValue(v.hostName);
        port.setValue(Integer.toString(v.port));
        workingDir.setValue(v.workingDirectory);
        clusterKey.setValue(v.clusterKey == null ? "" : v.clusterKey.getKey());
    }

    @Override
    public Class<HostKey> keyClass() {
        return HostKey.class;
    }

    @Override
    public Class<Host> valueClass() {
        return Host.class;
    }

    @Override
    public Class<HostFilter> filterClass() {
        return HostFilter.class;
    }

    @Override
    public HostKey key(String key) {
        return new HostKey(key);
    }

    @Override
    public Class<? extends ConcurrentSkipListMap<String, TimestampedValue<Host>>> responseClass() {
        return Results.class;
    }

    public static class Results extends ConcurrentSkipListMap<String, TimestampedValue<Host>> {
    }
}