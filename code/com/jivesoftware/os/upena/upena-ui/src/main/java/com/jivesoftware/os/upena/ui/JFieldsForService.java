package com.jivesoftware.os.upena.ui;

import com.jivesoftware.os.amza.shared.TimestampedValue;
import com.jivesoftware.os.upena.shared.Service;
import com.jivesoftware.os.upena.shared.ServiceFilter;
import com.jivesoftware.os.upena.shared.ServiceKey;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class JFieldsForService implements JObjectFields<ServiceKey, Service, ServiceFilter> {

    Map<String, JField> fields = new LinkedHashMap<>();
    String key;
    Service value;
    JEditKeyField serviceKey;
    JEditField name;
    JEditField description;

    public JFieldsForService(String k, Service v) {
        this.key = k;
        this.value = v;

        name = new JEditField("name", (v != null) ? v.name : "");
        fields.put("name", name);

        description = new JEditField("description", (v != null) ? v.description : "");
        fields.put("description", description);

        serviceKey = new JEditKeyField("key", (k != null) ? k : "");
        fields.put("key", serviceKey);

    }

    @Override
    public JObjectFields<ServiceKey, Service, ServiceFilter> copy() {
        return new JFieldsForService(key, value);
    }

    @Override
    public String shortName(Service v) {
        return v.name;
    }

    @Override
    public Service fieldsToObject() {
        return new Service(name.getValue(),
                description.getValue());
    }

    @Override
    public Map<String, JField> objectFields() {
        return fields;
    }

    @Override
    public ServiceKey key() {
        return new ServiceKey(serviceKey.getValue());
    }

    @Override
    public ServiceFilter fieldsToFilter() {
        ServiceFilter filter = new ServiceFilter(name.getValue(),
                description.getValue(), 0, 100);
        return filter;
    }

    @Override
    public void update(ServiceKey key, Service v) {
        serviceKey.setValue(key.getKey());
        name.setValue(v.name);
        description.setValue(v.description);
    }

    @Override
    public void updateFilter(ServiceKey key, Service v) {
        name.setValue(v.name);
        description.setValue(v.description);
    }

    @Override
    public Class<ServiceKey> keyClass() {
        return ServiceKey.class;
    }

    @Override
    public Class<Service> valueClass() {
        return Service.class;
    }

    @Override
    public Class<ServiceFilter> filterClass() {
        return ServiceFilter.class;
    }

    @Override
    public ServiceKey key(String key) {
        return new ServiceKey(key);
    }

    @Override
    public Class<? extends ConcurrentSkipListMap<String, TimestampedValue<Service>>> responseClass() {
        return Results.class;
    }

    public static class Results extends ConcurrentSkipListMap<String, TimestampedValue<Service>> {
    }
}