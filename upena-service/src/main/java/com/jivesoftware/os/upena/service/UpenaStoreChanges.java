/*
 * Copyright 2016 jonathan.colt.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jivesoftware.os.upena.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.upena.amza.shared.RowChanges;
import com.jivesoftware.os.upena.amza.shared.RowIndexKey;
import com.jivesoftware.os.upena.amza.shared.RowIndexValue;
import com.jivesoftware.os.upena.amza.shared.RowsChanged;
import com.jivesoftware.os.upena.shared.BasicTimestampedValue;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;

/**
 *
 * @author jonathan.colt
 */
class UpenaStoreChanges<K, V> implements RowChanges {

    private final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final ObjectMapper mapper;
    private final Class<K> keyClass;
    private final Class<V> valueClass;
    private final KeyValueChange<K, V> adds;
    private final KeyValueChange<K, V> removes;

    public UpenaStoreChanges(ObjectMapper mapper, Class<K> keyClass, Class<V> valueClass, KeyValueChange<K, V> adds, KeyValueChange<K, V> removes) {
        this.mapper = mapper;
        this.keyClass = keyClass;
        this.valueClass = valueClass;
        this.adds = adds;
        this.removes = removes;
    }

    @Override
    public void changes(RowsChanged changes) throws Exception {
        NavigableMap<RowIndexKey, RowIndexValue> appliedRows = changes.getApply();
        for (Map.Entry<RowIndexKey, RowIndexValue> entry : appliedRows.entrySet()) {
            RowIndexKey rawKey = entry.getKey();
            RowIndexValue rawValue = entry.getValue();
            if (entry.getValue().getTombstoned() && removes != null) {
                Collection<RowIndexValue> got = changes.getClobbered().get(rawKey);
                if (got != null) {
                    for (RowIndexValue g : got) {
                        K k = null;
                        try {
                            k = rawKey.getKey() == null ? null : mapper.readValue(rawKey.getKey(), keyClass);
                        } catch (Exception x) {
                            LOG.warn("Failed converting key {} of class {} to class {}",
                                new Object[]{rawKey.getKey(), rawKey.getKey() != null ? rawKey.getKey().getClass() : "null", keyClass}, x);
                            throw x;
                        }
                        V v = null;
                        try {
                            if (g.getValue() == null) {
                                v = null;
                            } else {
                                v = mapper.readValue(g.getValue(), valueClass);
                            }
                        } catch (Exception x) {
                            LOG.warn("Failed converting value {} of class {} to class {}",
                                new Object[]{g.getValue(), g.getValue() != null ? g.getValue().getClass() : "null", valueClass}, x);
                            //throw x;
                        }
                        removes.change(k, new BasicTimestampedValue<>(v, g.getTimestampId(), g.getTombstoned()));
                    }
                }
            } else if (adds != null) {
                K k = null;
                try {
                    k = rawKey.getKey() == null ? null : mapper.readValue(rawKey.getKey(), keyClass);
                } catch (Exception x) {
                    LOG.warn("Failed converting key {} of class {} to class {}",
                        new Object[]{rawKey.getKey(), rawKey.getKey() != null ? rawKey.getKey().getClass() : "null", keyClass}, x);
                    throw x;
                }
                V v = null;
                try {
                    v = rawValue.getValue() == null ? null : mapper.readValue(rawValue.getValue(), valueClass);
                } catch (Exception x) {
                    LOG.warn("Failed converting value {} of class {} to class {}",
                        new Object[]{rawValue.getValue(), rawValue.getValue() != null ? rawValue.getValue().getClass() : "null", valueClass}, x);
                    throw x;
                }
                adds.change(k, new BasicTimestampedValue<>(v, rawValue.getTimestampId(), rawValue.getTombstoned()));
            }
        }
    }
}
