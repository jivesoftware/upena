package com.jivesoftware.os.upena.service;

import com.jivesoftware.os.amza.shared.TimestampedValue;

public interface KeyValueChange<K, V> {

    void change(K key, TimestampedValue<V> is) throws Exception;
}
