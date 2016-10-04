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
import com.google.common.collect.Maps;
import com.jivesoftware.os.upena.amza.service.AmzaService;
import com.jivesoftware.os.upena.amza.service.AmzaTable;
import com.jivesoftware.os.upena.amza.shared.RowIndexKey;
import com.jivesoftware.os.upena.amza.shared.TableName;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UpenaConfigStore {

    private final AmzaService amzaService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, FetchedVersion> lastFetchedVersion = Maps.newConcurrentMap();

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
        lastFetchedVersion.remove(key);
    }

    public void putAll(String instanceKey, String context, Map<String, String> properties) throws Exception {
        AmzaTable partition = getPartition();
        String key = createTableName(instanceKey, context);
        RowIndexKey tableIndexKey = new RowIndexKey(key.getBytes("utf-8"));
        byte[] rawProperties = partition.get(tableIndexKey);
        if (rawProperties == null) {
            partition.set(tableIndexKey, mapper.writeValueAsBytes(properties));
        } else {
            Map<String, String> current = mapper.readValue(rawProperties, new TypeReference<HashMap<String, String>>() {
            });
            boolean changed = false;
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String existing = current.put(entry.getKey(), entry.getValue());
                if (existing == null || !existing.equals(entry.getValue())) {
                    changed = true;
                }
            }
            if (changed) {
                partition.set(tableIndexKey, mapper.writeValueAsBytes(current));
            }
        }
        lastFetchedVersion.remove(key);
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
        lastFetchedVersion.remove(key);
    }

    public Map<String, String> changesSinceLastFetch(String instanceKey, String context) throws Exception {
        String key = createTableName(instanceKey, context);
        FetchedVersion fetchedVersion = lastFetchedVersion.get(key);
        if (fetchedVersion == null || fetchedVersion.rawProperties == null) {
            return Collections.emptyMap();
        }
        Map<String, String> current = mapper.readValue(fetchedVersion.rawProperties, new TypeReference<HashMap<String, String>>() {
        });

        Map<String, String> changed = new HashMap<>();
        Map<String, String> stored = get(instanceKey, context, null, false);

        for (String c : current.keySet()) {
            String currentValue = current.get(c);
            String storedValue = stored.get(c);

            if (storedValue == null) {
                changed.put(c, currentValue + " is not null");
            } else if (!storedValue.equals(currentValue)) {
                changed.put(c, currentValue + " is now " + storedValue);
            }
        }

        return changed;

    }

    public Map<String, String> get(String instanceKey, String context, List<String> keys, boolean cacheFetchedVersion) throws Exception {
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
        if (cacheFetchedVersion) {
            lastFetchedVersion.put(key, new FetchedVersion(System.currentTimeMillis(), rawProperties));
        }
        return results;
    }

    public static class FetchedVersion {

        public final long time;
        public final byte[] rawProperties;

        public FetchedVersion(long time, byte[] rawProperties) {
            this.time = time;
            this.rawProperties = rawProperties;
        }

    }
}
