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

import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupFilter;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class JFieldsForReleaseGroup implements JObjectFields<ReleaseGroupKey, ReleaseGroup, ReleaseGroupFilter> {

    Map<String, JField> fields = new LinkedHashMap<>();
    JObjectFactory factory;
    String key;
    ReleaseGroup value;
    JEditKeyField releaseGroupKey;
    JEditField name;
    JEditField email;
    JEditField version;
    JEditField description;

    public JFieldsForReleaseGroup(String k, ReleaseGroup v) {
        this.key = k;
        this.value = v;

        name = new JEditField("name", (v != null) ? v.name : "");
        fields.put("name", name);

        email = new JEditField("email", (v != null) ? v.email : "");
        fields.put("email", email);

        version = new JEditField("version", (v != null) ? v.version : "");
        fields.put("version", version);

        description = new JEditField("description", (v != null) ? v.description : "");
        fields.put("description", description);

        releaseGroupKey = new JEditKeyField("key", (k != null) ? k : "");
        fields.put("key", releaseGroupKey);

    }

    @Override
    public JObjectFields<ReleaseGroupKey, ReleaseGroup, ReleaseGroupFilter> copy() {
        return new JFieldsForReleaseGroup(key, value);
    }

    @Override
    public String shortName(ReleaseGroup v) {
        return v.name;
    }

    @Override
    public ReleaseGroup fieldsToObject() {
        return new ReleaseGroup(name.getValue(),
                email.getValue(),
                version.getValue(),
                description.getValue());
    }

    @Override
    public Map<String, JField> objectFields() {
        return fields;
    }

    @Override
    public ReleaseGroupKey key() {
        return new ReleaseGroupKey(releaseGroupKey.getValue());
    }

    @Override
    public ReleaseGroupFilter fieldsToFilter() {
        ReleaseGroupFilter filter = new ReleaseGroupFilter(name.getValue(),
                email.getValue(),
                version.getValue(),
                description.getValue(), 0, 100);
        return filter;
    }

    @Override
    public void update(ReleaseGroupKey key, ReleaseGroup v) {
        releaseGroupKey.setValue(key.getKey());
        name.setValue(v.name);
        email.setValue(v.email);
        version.setValue(v.version);
        description.setValue(v.description);
    }

    @Override
    public void updateFilter(ReleaseGroupKey key, ReleaseGroup v) {
        name.setValue(v.name);
        email.setValue(v.email);
        version.setValue(v.version);
        description.setValue(v.description);
    }

    @Override
    public Class<ReleaseGroupKey> keyClass() {
        return ReleaseGroupKey.class;
    }

    @Override
    public Class<ReleaseGroup> valueClass() {
        return ReleaseGroup.class;
    }

    @Override
    public Class<ReleaseGroupFilter> filterClass() {
        return ReleaseGroupFilter.class;
    }

    @Override
    public ReleaseGroupKey key(String key) {
        return new ReleaseGroupKey(key);
    }

    @Override
    public Class<? extends ConcurrentSkipListMap<String, TimestampedValue<ReleaseGroup>>> responseClass() {
        return Results.class;
    }

    public static class Results extends ConcurrentSkipListMap<String, TimestampedValue<ReleaseGroup>> {
    }
}