package com.jivesoftware.os.upena.config;

import com.jivesoftware.os.amza.service.AmzaService;
import com.jivesoftware.os.amza.service.AmzaTable;
import com.jivesoftware.os.amza.shared.TableName;
import com.jivesoftware.os.amza.shared.TimestampedValue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UpenaConfigStore {

    private final AmzaService amzaService;

    public UpenaConfigStore(AmzaService amzaService) {
        this.amzaService = amzaService;
    }

    private String createTableName(String instanceKey, String context) {
        return "upena.config." + instanceKey + "/" + context;
    }

    synchronized private AmzaTable<String, String> getPartition(String instanceKey, String context) throws Exception {

        String pname = createTableName(instanceKey, context);
        TableName<String, String> tableName = new TableName<>("master",
                pname,
                String.class, null, null, String.class);
        return amzaService.getTable(tableName);
    }

    synchronized public void remove(String instanceKey) throws Exception {
        Map<TableName, AmzaTable> tables = amzaService.getTables();
        for (Map.Entry<TableName, AmzaTable> entry : tables.entrySet()) {
            TableName tableName = entry.getKey();
            if (tableName.getTableName().startsWith("upena.config." + instanceKey)) {
                amzaService.destroyTable(tableName);
            }
        }
    }

    public void set(String releaseGroupKey, String context, Map<String, String> properties) throws Exception {
        getPartition(releaseGroupKey, context).set(properties.entrySet()); // TODO only update if changed.
    }

    public void remove(String releaseGroupKey, String context, Set<String> keys) throws Exception {
        getPartition(releaseGroupKey, context).remove(keys); // TODO only update if changed.
    }

    public Map<String, String> get(String instanceKey, String context, List<String> keys) throws Exception {
        final Map<String, String> results = new HashMap<>();
        if (keys != null && !keys.isEmpty()) {
            getPartition(instanceKey, context).get(keys, new AmzaTable.ValueStream<Map.Entry<String, String>>() {

                @Override
                public Map.Entry<String, String> stream(Map.Entry<String, String> value) {
                    if (value != null) {
                        results.put(value.getKey(), value.getValue());
                    }
                    return value;
                }
            });
            return results;
        } else {
            AmzaTable<String, String> partition = getPartition(instanceKey, context);
            partition.listEntries(new AmzaTable.ValueStream<Map.Entry<String, TimestampedValue<String>>>() {

                @Override
                public Map.Entry<String, TimestampedValue<String>> stream(Map.Entry<String, TimestampedValue<String>> value) {
                    if (value != null && !value.getValue().getTombstoned()) {
                        results.put(value.getKey(), value.getValue().getValue());
                    }
                    return value;
                }
            });
            return results;
        }
    }
}