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