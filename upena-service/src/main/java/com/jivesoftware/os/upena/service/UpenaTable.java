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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.amza.service.AmzaTable;
import com.jivesoftware.os.upena.amza.shared.RowIndexKey;
import com.jivesoftware.os.upena.shared.BasicTimestampedValue;
import com.jivesoftware.os.upena.shared.Key;
import com.jivesoftware.os.upena.shared.KeyValueFilter;
import com.jivesoftware.os.upena.shared.Stored;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;

public class UpenaTable<K extends Key, V extends Stored> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    static public interface UpenaKeyProvider<KK extends Key, VV extends Stored> {

        KK getNodeKey(UpenaTable<KK, VV> table, VV value);
    }

    static public interface UpenaValueValidator<KK extends Key, VV extends Stored> {

        VV validate(UpenaTable<KK, VV> table, KK key, VV value) throws Exception;
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    private final AmzaTable store;
    private final Class<K> keyClass;
    private final Class<V> valueClass;
    private final UpenaKeyProvider<K, V> keyProvider;
    private final UpenaValueValidator<K, V> valueValidator;

    public UpenaTable(AmzaTable store,
        Class<K> keyClass,
        Class<V> valueClass,
        UpenaKeyProvider<K, V> keyProvider,
        UpenaValueValidator<K, V> valueValidator) {
        this.store = store;
        this.keyClass = keyClass;
        this.valueClass = valueClass;
        this.keyProvider = keyProvider;
        this.valueValidator = valueValidator;
    }

    public V get(K key) throws Exception {
        byte[] rawKey = mapper.writeValueAsBytes(key);
        byte[] got = store.get(new RowIndexKey(rawKey));
        if (got == null) {
            return null;
        }
        return mapper.readValue(got, valueClass);
    }

    public void scan(final Stream<K, V> stream) throws Exception {
        store.scan((l, key, value) -> {
            if (!value.getTombstoned()) {
                K k = mapper.readValue(key.getKey(), keyClass);
                V v = mapper.readValue(value.getValue(), valueClass);
                return stream.stream(k, v);
            } else {
                return true;
            }
        });
    }

    public interface Stream<K, V> {

        boolean stream(K key, V value) throws Exception;
    }

    @SuppressWarnings("unchecked")
    public ConcurrentNavigableMap<K, TimestampedValue<V>> find(final KeyValueFilter<K, V> filter) throws Exception {
        final ConcurrentNavigableMap<K, TimestampedValue<V>> results = filter.createCollector();
        List<RowIndexKey> badKeys = Lists.newArrayList();
        store.scan((transactionId, key, value) -> {
            try {
                if (!value.getTombstoned()) {
                    K k = mapper.readValue(key.getKey(), keyClass);
                    V v = mapper.readValue(value.getValue(), valueClass);

                    if (filter.filter(k, v)) {
                        results.put(k, new BasicTimestampedValue(v, value.getTimestampId(), value.getTombstoned()));
                    }
                }
            } catch (Exception x) {
                LOG.warn("Failed to scan. {}", new Object[]{filter}, x);
                badKeys.add(key);
            }
            return true;
        });
        if (!badKeys.isEmpty()) {
            for (RowIndexKey key : badKeys) {
                store.remove(key);
            }
            LOG.info("Removed {} bad keys", badKeys.size());
        }
        return results;
    }

    synchronized public K update(K key, V value) throws Exception {
        if (key == null) {
            key = keyProvider.getNodeKey(this, value);
        }
        if (valueValidator != null) {
            value = valueValidator.validate(this, key, value);
        }
        byte[] rawKey = mapper.writeValueAsBytes(key);
        byte[] rawValue = mapper.writeValueAsBytes(value);
        RowIndexKey set = store.set(new RowIndexKey(rawKey), rawValue);
        K gotKey = mapper.readValue(set.getKey(), keyClass);
        return gotKey;
    }

    synchronized public boolean remove(K key) throws Exception {
        byte[] rawKey = mapper.writeValueAsBytes(key);
        return store.remove(new RowIndexKey(rawKey));
    }
}
