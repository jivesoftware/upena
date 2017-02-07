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
package com.jivesoftware.os.upena.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.jivesoftware.os.amza.api.partition.Consistency;
import com.jivesoftware.os.amza.api.partition.Durability;
import com.jivesoftware.os.amza.api.partition.PartitionName;
import com.jivesoftware.os.amza.api.partition.PartitionProperties;
import com.jivesoftware.os.amza.api.stream.RowType;
import com.jivesoftware.os.amza.service.AmzaService;
import com.jivesoftware.os.amza.service.EmbeddedClientProvider;
import com.jivesoftware.os.amza.service.EmbeddedClientProvider.CheckOnline;
import com.jivesoftware.os.amza.service.EmbeddedClientProvider.EmbeddedClient;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.amza.service.AmzaTable;
import com.jivesoftware.os.upena.amza.service.UpenaAmzaService;
import com.jivesoftware.os.upena.amza.shared.TableName;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class UpenaConfigStore {

    public static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final ObjectMapper mapper;
    private final UpenaAmzaService upenaAmzaService;
    private final AmzaService amzaService;
    private final EmbeddedClientProvider embeddedClientProvider;
    private final Map<String, FetchedVersion> lastFetchedVersion = Maps.newConcurrentMap();

    private final PartitionProperties partitionProperties = new PartitionProperties(Durability.fsync_async,
        TimeUnit.DAYS.toMillis(30), TimeUnit.DAYS.toMillis(10), TimeUnit.DAYS.toMillis(30), TimeUnit.DAYS.toMillis(10),
        0, 0, 0, 0,
        false, Consistency.quorum, true, true, false, RowType.snappy_primary, "lab", -1, null, -1, -1);

    public UpenaConfigStore(ObjectMapper mapper,
        UpenaAmzaService upenaAmzaService,
        AmzaService amzaService,
        EmbeddedClientProvider embeddedClientProvider) throws Exception {

        this.mapper = mapper;
        this.upenaAmzaService = upenaAmzaService;
        this.amzaService = amzaService;
        this.embeddedClientProvider = embeddedClientProvider;
    }


    public void init() throws Exception {

        if (upenaAmzaService != null) {
            EmbeddedClient client = client();
            TableName tableName = new TableName("master", "config", null, null);
            AmzaTable table = upenaAmzaService.getTable(tableName);
            long[] count = { 0 };
            table.scan((transactionId, key, value) -> {
                count[0]++;
                client.commit(Consistency.quorum, null,
                    commitKeyValueStream -> commitKeyValueStream.commit(key.getKey(), value.getValue(), value.getTimestampId(), value.getTombstoned()),
                    30_000, TimeUnit.MILLISECONDS);
                return true;
            });
            LOG.info("UPGRADE: carried {} configs forward.", count[0]);
        }
    }

    private EmbeddedClient client() throws Exception {
        PartitionName partitionName = new PartitionName(false, "upena".getBytes(), ("upena-config").getBytes());
        while (true) {
            try {
                amzaService.getRingWriter().ensureMaximalRing(partitionName.getRingName(), 30_000L); //TODO config
                amzaService.createPartitionIfAbsent(partitionName, partitionProperties);
                amzaService.awaitOnline(partitionName, 30_000L); //TODO config
                return embeddedClientProvider.getClient(partitionName, CheckOnline.once);
            } catch (Exception x) {
                LOG.warn("Failed to get client for " + partitionName.getName() + ". Retrying...", x);
            }
        }
    }


    private String createTableName(String instanceKey, String context) {
        return "config/" + instanceKey + "/" + context;
    }


    synchronized private byte[] get(String instanceKey, String context) throws Exception {
        String key = createTableName(instanceKey, context);
        return client().getValue(Consistency.none, null, key.getBytes(StandardCharsets.UTF_8));
    }

    synchronized private void set(String instanceKey, String context, byte[] rawProperties) throws Exception {
        String key = createTableName(instanceKey, context);
        client().commit(Consistency.quorum, null,
            commitKeyValueStream -> commitKeyValueStream.commit(key.getBytes(StandardCharsets.UTF_8), rawProperties, -1L, false),
            30_000, TimeUnit.MILLISECONDS);
    }


    synchronized public void remove(String instanceKey, String context) throws Exception {
        String key = createTableName(instanceKey, context);
        client().commit(Consistency.quorum, null,
            commitKeyValueStream -> commitKeyValueStream.commit(key.getBytes(StandardCharsets.UTF_8), null, -1L, true),
            30_000, TimeUnit.MILLISECONDS);
        lastFetchedVersion.remove(key);
    }


    public void putAll(String instanceKey, String context, Map<String, String> properties) throws Exception {
        String key = createTableName(instanceKey, context);
        byte[] rawProperties = get(instanceKey, context);
        if (rawProperties == null) {
            set(instanceKey, context, mapper.writeValueAsBytes(properties));
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
                set(instanceKey, context, mapper.writeValueAsBytes(current));
            }
        }
        lastFetchedVersion.remove(key);
    }

    public void remove(String instanceKey, String context, Set<String> keys) throws Exception {
        String key = createTableName(instanceKey, context);
        byte[] rawProperties = get(instanceKey, context);
        if (rawProperties != null) {
            Map<String, String> current = mapper.readValue(rawProperties, new TypeReference<HashMap<String, String>>() {
            });
            for (String k : keys) {
                current.remove(k);
            }
            set(instanceKey, context, mapper.writeValueAsBytes(current));
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
        Map<String, String> results = new HashMap<>();
        String key = createTableName(instanceKey, context);
        byte[] rawProperties = get(instanceKey, context);
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
