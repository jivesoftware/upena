package com.jivesoftware.os.upena.ui;

import com.jivesoftware.os.amza.shared.TimestampedValue;
import com.jivesoftware.os.upena.shared.ReleaseGroup;
import com.jivesoftware.os.upena.shared.ReleaseGroupKey;
import com.jivesoftware.os.upena.shared.Tenant;
import com.jivesoftware.os.upena.shared.TenantFilter;
import com.jivesoftware.os.upena.shared.TenantKey;
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
    JEditRef releaseGroupKey;

    public JFieldsTenant(JObjectFactory factory, String k, Tenant tenant) {
        this.factory = factory;
        this.key = k;
        this.value = tenant;

        tenantId = new JEditField("tenantId", (tenant != null) ? tenant.tenantId : "");
        fields.put("tenantId", tenantId);

        description = new JEditField("description", (tenant != null) ? tenant.description : "");
        fields.put("description", description);

        releaseGroupKey = new JEditRef(factory, "releaseGroupKey", ReleaseGroup.class,
                (tenant != null) ? (tenant.releaseGroupKey != null) ? tenant.releaseGroupKey.getKey() : "" : "");
        fields.put("releaseGroupKey", releaseGroupKey);

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
        return new Tenant(tenantId.getValue(),
                description.getValue(),
                new ReleaseGroupKey(releaseGroupKey.getValue()));
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
                description.getValue(),
                FilterUtils.nullIfEmpty(new ReleaseGroupKey(releaseGroupKey.getValue())), 0, 100);
        return filter;
    }

    @Override
    public void update(TenantKey key, Tenant v) {
        tenantKey.setValue(key.getKey());
        tenantId.setValue(v.tenantId);
        description.setValue(v.description);
        releaseGroupKey.setValue(v.releaseGroupKey.getKey());
    }

    @Override
    public void updateFilter(TenantKey key, Tenant v) {
        tenantId.setValue(v.tenantId);
        description.setValue(v.description);
        releaseGroupKey.setValue(v.releaseGroupKey.getKey());
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