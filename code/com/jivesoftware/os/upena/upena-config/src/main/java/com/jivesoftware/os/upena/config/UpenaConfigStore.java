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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.amza.service.AmzaService;
import com.jivesoftware.os.amza.service.AmzaTable;
import com.jivesoftware.os.amza.shared.RowIndexKey;
import com.jivesoftware.os.amza.shared.TableName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UpenaConfigStore {

    private final AmzaService amzaService;
    private final ObjectMapper mapper = new ObjectMapper();

    public UpenaConfigStore(AmzaService amzaService) {
        this.amzaService = amzaService;
    }

    private String createTableName(String instanceKey, String context) {
        return "config/" + instanceKey + "/" + context;
    }

    synchronized private AmzaTable getPartition() throws Exception {

        TableName tableName = new TableName("master", "config", null, null);
        return amzaService.getTable(tableName);
    }

    synchronized public void remove(String instanceKey, String context) throws Exception {
        AmzaTable partition = getPartition();
        String key = createTableName(instanceKey, context);
        partition.remove(new RowIndexKey(key.getBytes("utf-8")));
    }

    public void set(String instanceKey, String context, Map<String, String> properties) throws Exception {
        AmzaTable partition = getPartition();
        String key = createTableName(instanceKey, context);
        RowIndexKey tableIndexKey = new RowIndexKey(key.getBytes("utf-8"));
        byte[] rawProperties = partition.get(tableIndexKey);
        if (rawProperties == null) {
            partition.set(tableIndexKey, mapper.writeValueAsBytes(properties));
        } else {
            Map<String, String> current = mapper.readValue(rawProperties, new TypeReference<HashMap<String, String>>() {
            });
            current.putAll(properties);
            partition.set(tableIndexKey, mapper.writeValueAsBytes(current));
        }
    }

    public void remove(String instanceKey, String context, Set<String> keys) throws Exception {
        AmzaTable partition = getPartition();
        String key = createTableName(instanceKey, context);
        RowIndexKey tableIndexKey = new RowIndexKey(key.getBytes("utf-8"));
        byte[] rawProperties = partition.get(tableIndexKey);
        if (rawProperties != null) {
            Map<String, String> current = mapper.readValue(rawProperties, new TypeReference<HashMap<String, String>>() {
            });
            for (String k : keys) {
                current.remove(k);
            }
            partition.set(tableIndexKey, mapper.writeValueAsBytes(current));
        }
    }

    public Map<String, String> get(String instanceKey, String context, List<String> keys) throws Exception {
        final Map<String, String> results = new HashMap<>();
        AmzaTable partition = getPartition();
        String key = createTableName(instanceKey, context);
        RowIndexKey tableIndexKey = new RowIndexKey(key.getBytes("utf-8"));
        byte[] rawProperties = partition.get(tableIndexKey);
        if (rawProperties != null) {
            Map<String, String> current = mapper.readValue(rawProperties, new TypeReference<HashMap<String, String>>() {
            });
            if (keys != null && !keys.isEmpty()) {
                for (String k : keys) {
                    String v = current.get(k);
                    if (v != null) {
                        results.put(k, v);
                    }
                }
            } else {
                results.putAll(current);
            }
        }
        return results;
    }
}
