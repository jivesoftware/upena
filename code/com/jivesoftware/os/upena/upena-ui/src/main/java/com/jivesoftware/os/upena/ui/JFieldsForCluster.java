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

import com.jivesoftware.os.amza.shared.TimestampedValue;
import com.jivesoftware.os.upena.shared.Cluster;
import com.jivesoftware.os.upena.shared.ClusterFilter;
import com.jivesoftware.os.upena.shared.ClusterKey;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.ServiceKey;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class JFieldsForCluster implements JObjectFields<ClusterKey, Cluster, ClusterFilter> {

    Map<String, JField> fields = new LinkedHashMap<>();
    JObjectFactory factory;
    String key;
    Cluster value;
    JEditKeyField clusterKey;
    JEditField name;
    JEditField description;
    JEditReleaseGroupsField defaultReleaseGroups;

    public JFieldsForCluster(JObjectFactory factory, String k, Cluster v) {
        this.factory = factory;
        this.key = k;
        this.value = v;
        name = new JEditField("name", (v != null) ? v.name : "");
        fields.put("name", name);
        description = new JEditField("description", (v != null) ? v.description : "");
        fields.put("description", description);
        defaultReleaseGroups = new JEditReleaseGroupsField(factory, "defaultReleaseGroups", (v != null) ? v.defaultReleaseGroups : null);
        fields.put("defaultReleaseGroups", defaultReleaseGroups);
        clusterKey = new JEditKeyField("key", (k != null) ? k : "");
        fields.put("key", clusterKey);

    }

    @Override
    public JObjectFields<ClusterKey, Cluster, ClusterFilter> copy() {
        return new JFieldsForCluster(factory, key, value);
    }

    @Override
    public String shortName(Cluster v) {
        return v.name;
    }

    @Override
    public Cluster fieldsToObject() {
        Map<ServiceKey, ReleaseGroupKey> defaults = new HashMap<>();
        defaults.putAll(defaultReleaseGroups.field);
        Cluster cluster = new Cluster(name.getValue(),
                description.getValue(),
                defaults);
        return cluster;
    }

    @Override
    public Map<String, JField> objectFields() {
        return fields;
    }

    @Override
    public ClusterKey key() {
        return new ClusterKey(clusterKey.getValue());
    }

    @Override
    public ClusterFilter fieldsToFilter() {
        ClusterFilter filter = new ClusterFilter(name.getValue(),
                description.getValue(), 0, 100);
        return filter;
    }

    @Override
    public void update(ClusterKey key, Cluster v) {
        clusterKey.setValue(key.getKey());
        name.setValue(v.name);
        description.setValue(v.description);
        defaultReleaseGroups.setValue(v.defaultReleaseGroups);
    }

    @Override
    public void updateFilter(ClusterKey key, Cluster v) {
        name.setValue(v.name);
        description.setValue(v.description);
    }

    @Override
    public Class<ClusterKey> keyClass() {
        return ClusterKey.class;
    }

    @Override
    public Class<Cluster> valueClass() {
        return Cluster.class;
    }

    @Override
    public Class<ClusterFilter> filterClass() {
        return ClusterFilter.class;
    }

    @Override
    public ClusterKey key(String key) {
        return new ClusterKey(key);
    }

    @Override
    public Class<? extends ConcurrentSkipListMap<String, TimestampedValue<Cluster>>> responseClass() {
        return Results.class;
    }

    public static class Results extends ConcurrentSkipListMap<String, TimestampedValue<Cluster>> {
    }
}