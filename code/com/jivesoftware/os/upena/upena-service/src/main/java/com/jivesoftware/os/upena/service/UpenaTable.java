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

import com.jivesoftware.os.amza.service.AmzaTable;
import com.jivesoftware.os.amza.shared.KeyValueFilter;
import com.jivesoftware.os.amza.shared.TimestampedValue;
import com.jivesoftware.os.upena.shared.Key;
import com.jivesoftware.os.upena.shared.Stored;
import java.util.concurrent.ConcurrentNavigableMap;

public class UpenaTable<K extends Key, V extends Stored> {

    static public interface UpenaKeyProvider<KK extends Key, VV extends Stored> {

        KK getNodeKey(UpenaTable<KK, VV> table, VV value);
    }

    static public interface UpenaValueValidator<KK extends Key, VV extends Stored> {

        VV valiadate(UpenaTable<KK, VV> table, KK key, VV value) throws Exception;
    }

    private final AmzaTable<K, V> store;
    private final UpenaKeyProvider<K, V> keyProvider;
    private final UpenaValueValidator<K, V> valueValidator;

    public UpenaTable(AmzaTable<K, V> store, UpenaKeyProvider<K, V> nodeKeyProvider, UpenaValueValidator<K, V> valueValidator) {
        this.store = store;
        this.keyProvider = nodeKeyProvider;
        this.valueValidator = valueValidator;
    }

    public K toKey(V value) {
        return keyProvider.getNodeKey(this, value);
    }

    public V get(K key) throws Exception {
        return store.get(key);
    }

    ConcurrentNavigableMap<K, TimestampedValue<V>> find(KeyValueFilter<K, V> filter) throws Exception {
        return store.filter(filter);
    }

    synchronized public K update(K key, V value) throws Exception {
        if (key == null) {
            key = keyProvider.getNodeKey(this, value);
        }
        if (valueValidator != null) {
            value = valueValidator.valiadate(this, key, value);
        }
        return store.set(key, value);
    }

    synchronized public boolean remove(K key) throws Exception {
        return store.remove(key);
    }
}
