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

import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.ServiceKey;
import com.jivesoftware.os.upena.shared.Tenant;
import com.jivesoftware.os.upena.shared.TenantFilter;
import com.jivesoftware.os.upena.shared.TenantKey;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class JFieldsTenant implements JObjectFields<TenantKey, Tenant, TenantFilter> {

    Map<String, JField> fields = new LinkedHashMap<>();
    JObjectFactory factory;
    String key;
    Tenant value;
    JEditKeyField tenantKey;
    JEditField tenantId;
    JEditField description;
    JEditReleaseGroupsField overrideReleaseGroups;

    public JFieldsTenant(JObjectFactory factory, String k, Tenant v) {
        this.factory = factory;
        this.key = k;
        this.value = v;

        tenantId = new JEditField("tenantId", (v != null) ? v.tenantId : "");
        fields.put("tenantId", tenantId);

        description = new JEditField("description", (v != null) ? v.description : "");
        fields.put("description", description);

        overrideReleaseGroups = new JEditReleaseGroupsField(factory, "overrideReleaseGroups", (v != null) ? v.overrideReleaseGroups : null);
        fields.put("overrideReleaseGroups", overrideReleaseGroups);

        tenantKey = new JEditKeyField("key", (k != null) ? k : "");
        fields.put("key", tenantKey);

    }

    @Override
    public JObjectFields<TenantKey, Tenant, TenantFilter> copy() {
        return new JFieldsTenant(factory, key, value);
    }

    @Override
    public String shortName(Tenant v) {
        return v.tenantId;
    }

    @Override
    public Tenant fieldsToObject() {

        Map<ServiceKey, ReleaseGroupKey> overrides = new HashMap<>();
        overrides.putAll(overrideReleaseGroups.field);

        return new Tenant(tenantId.getValue(),
            description.getValue(),
            overrides);
    }

    @Override
    public Map<String, JField> objectFields() {
        return fields;
    }

    @Override
    public TenantKey key() {
        return new TenantKey(tenantKey.getValue());
    }

    @Override
    public TenantFilter fieldsToFilter() {
        TenantFilter filter = new TenantFilter(tenantId.getValue(),
            description.getValue(), 0, 100);
        return filter;
    }

    @Override
    public void update(TenantKey key, Tenant v) {
        tenantKey.setValue(key.getKey());
        tenantId.setValue(v.tenantId);
        description.setValue(v.description);
        overrideReleaseGroups.setValue(v.overrideReleaseGroups);
    }

    @Override
    public void updateFilter(TenantKey key, Tenant v) {
        tenantId.setValue(v.tenantId);
        description.setValue(v.description);
    }

    @Override
    public Class<TenantKey> keyClass() {
        return TenantKey.class;
    }

    @Override
    public Class<Tenant> valueClass() {
        return Tenant.class;
    }

    @Override
    public Class<TenantFilter> filterClass() {
        return TenantFilter.class;
    }

    @Override
    public TenantKey key(String key) {
        return new TenantKey(key);
    }

    @Override
    public Class<? extends ConcurrentSkipListMap<String, TimestampedValue<Tenant>>> responseClass() {
        return Results.class;
    }

    public static class Results extends ConcurrentSkipListMap<String, TimestampedValue<Tenant>> {
    }
}
