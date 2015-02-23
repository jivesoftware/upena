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
import com.jivesoftware.os.upena.shared.HostKey;
import com.jivesoftware.os.upena.shared.Instance;
import com.jivesoftware.os.upena.shared.InstanceFilter;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class JFieldsForInstance implements JObjectFields<InstanceKey, Instance, InstanceFilter> {

    Map<String, JField> fields = new LinkedHashMap<>();
    JObjectFactory factory;
    String key;
    Instance value;
    JEditKeyField instanceKey;
    JEditField instanceId;
    JEditBooleanField enabled;
    JEditBooleanField locked;
    JEditRef releaseGroupKey;
    JEditRef serviceKey;
    JEditRef hostKey;
    JEditRef clusterKey;
    JEditPortsField ports;

    public JFieldsForInstance(JObjectFactory factory, String k, Instance v) {
        this.factory = factory;
        this.key = k;
        this.value = v;

        hostKey = new JEditRef(factory, "host", Host.class, (v != null) ? (v.hostKey != null) ? v.hostKey.getKey() : "" : "");
        fields.put("hostKey", hostKey);

        serviceKey = new JEditRef(factory, "service", Service.class, (v != null) ? (v.serviceKey != null) ? v.serviceKey.getKey() : "" : "");
        fields.put("serviceKey", serviceKey);

        instanceId = new JEditField("instanceId", (v != null) ? "" + v.instanceId : "");
        fields.put("instanceId", instanceId);

        clusterKey = new JEditRef(factory, "cluster", Cluster.class, (v != null) ? (v.clusterKey != null) ? v.clusterKey.getKey() : "" : "");
        fields.put("clusterKey", clusterKey);

        releaseGroupKey = new JEditRef(
            factory, "releaseGroup", ReleaseGroup.class, (v != null) ? (v.releaseGroupKey != null) ? v.releaseGroupKey.getKey() : "" : "");
        fields.put("releaseGroupKey", releaseGroupKey);

        enabled = new JEditBooleanField("enabled", "");
        fields.put("enabled", enabled);

        locked = new JEditBooleanField("locked", "");
        fields.put("locked", locked);

        ports = new JEditPortsField(factory, "ports", (v != null) ? (v.ports != null) ? v.ports : null : null);
        fields.put("ports", ports);

        instanceKey = new JEditKeyField("key", (k != null) ? k : "");
        fields.put("key", instanceKey);

    }

    @Override
    public JObjectFields<InstanceKey, Instance, InstanceFilter> copy() {
        return new JFieldsForInstance(factory, key, value);
    }

    @Override
    public String shortName(Instance v) {
        return "" + v.instanceId;
    }

    @Override
    public Instance fieldsToObject() {
        return new Instance(new ClusterKey(clusterKey.getValue()),
            new HostKey(hostKey.getValue()),
            new ServiceKey(serviceKey.getValue()),
            new ReleaseGroupKey(releaseGroupKey.getValue()),
            Integer.parseInt(instanceId.getValue()),
            Boolean.parseBoolean(enabled.getValue()),
            Boolean.parseBoolean(locked.getValue()),
            System.currentTimeMillis());

    }

    @Override
    public Map<String, JField> objectFields() {
        return fields;
    }

    @Override
    public InstanceKey key() {
        return new InstanceKey(instanceKey.getValue());
    }

    @Override
    public InstanceFilter fieldsToFilter() {
        Integer id = null;
        try {
            id = Integer.parseInt(instanceId.getValue());
        } catch (Exception x) {
        }
        InstanceFilter filter = new InstanceFilter(
            FilterUtils.nullIfEmpty(new ClusterKey(clusterKey.getValue())),
            FilterUtils.nullIfEmpty(new HostKey(hostKey.getValue())),
            FilterUtils.nullIfEmpty(new ServiceKey(serviceKey.getValue())),
            FilterUtils.nullIfEmpty(new ReleaseGroupKey(releaseGroupKey.getValue())),
            id,
            0, 100);
        return filter;
    }

    @Override
    public void update(InstanceKey key, Instance v) {
        instanceKey.setValue(key.getKey());
        instanceId.setValue("" + v.instanceId);
        clusterKey.setValue(v.clusterKey.getKey());
        serviceKey.setValue(v.serviceKey.getKey());
        releaseGroupKey.setValue(v.releaseGroupKey.getKey());
        hostKey.setValue(v.hostKey.getKey());
        enabled.setValue(Boolean.toString(v.enabled));
        locked.setValue(Boolean.toString(v.locked));
        ports.setValue(v.ports);

    }

    @Override
    public void updateFilter(InstanceKey key, Instance v) {
        instanceId.setValue("" + v.instanceId);
        clusterKey.setValue(v.clusterKey.getKey());
        serviceKey.setValue(v.serviceKey.getKey());
        releaseGroupKey.setValue(v.releaseGroupKey.getKey());
        hostKey.setValue(v.hostKey.getKey());
        enabled.setValue(Boolean.toString(v.enabled));
        locked.setValue(Boolean.toString(v.locked));
    }

    @Override
    public Class<InstanceKey> keyClass() {
        return InstanceKey.class;
    }

    @Override
    public Class<Instance> valueClass() {
        return Instance.class;
    }

    @Override
    public Class<InstanceFilter> filterClass() {
        return InstanceFilter.class;
    }

    @Override
    public InstanceKey key(String key) {
        return new InstanceKey(key);
    }

    @Override
    public Class<? extends ConcurrentSkipListMap<String, TimestampedValue<Instance>>> responseClass() {
        return Results.class;
    }

    public static class Results extends ConcurrentSkipListMap<String, TimestampedValue<Instance>> {
    }
}
