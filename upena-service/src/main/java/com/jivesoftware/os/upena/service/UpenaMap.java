package com.jivesoftware.os.upena.service;

import com.jivesoftware.os.upena.shared.Key;
import com.jivesoftware.os.upena.shared.KeyValueFilter;
import com.jivesoftware.os.upena.shared.Stored;
import com.jivesoftware.os.upena.shared.TimestampedValue;
import java.util.concurrent.ConcurrentNavigableMap;

/**
 * Created by jonathan.colt on 2/3/17.
 */
public interface UpenaMap<K extends Key, V extends Stored> {
    void putIfAbsent(K key, V value) throws Exception;

    V get(K key) throws Exception;

    void scan(Stream<K, V> stream) throws Exception;

    @SuppressWarnings("unchecked")
    ConcurrentNavigableMap<K, TimestampedValue<V>> find(boolean removeBadKeysEnabled, KeyValueFilter<K, V> filter) throws Exception;

    K update(K key, V value) throws Exception;

    boolean remove(K key) throws Exception;

    interface UpenaKeyProvider<KK extends Key, VV extends Stored> {

        KK getNodeKey(UpenaMap<KK, VV> table, VV value);
    }

    interface UpenaValueValidator<KK extends Key, VV extends Stored> {

        VV validate(UpenaMap<KK, VV> table, KK key, VV value) throws Exception;
    }

    interface Stream<K, V> {

        boolean stream(K key, V value) throws Exception;
    }
}
