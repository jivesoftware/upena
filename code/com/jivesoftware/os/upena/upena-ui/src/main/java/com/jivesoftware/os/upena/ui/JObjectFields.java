package com.jivesoftware.os.upena.ui;

import com.jivesoftware.os.amza.shared.KeyValueFilter;
import com.jivesoftware.os.amza.shared.TimestampedValue;
import com.jivesoftware.os.upena.shared.Key;
import com.jivesoftware.os.upena.shared.Stored;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public interface JObjectFields<K extends Key, V extends Stored, F extends KeyValueFilter<K, V>> {

    V fieldsToObject();

    Map<String, JField> objectFields();

    K key();

    K key(String key);

    F fieldsToFilter();

    void update(K key, V v);

    void updateFilter(K key, V v);

    Class<K> keyClass();

    Class<V> valueClass();

    Class<F> filterClass();

    Class<? extends ConcurrentSkipListMap<String, TimestampedValue<V>>> responseClass();

    String shortName(V v);

    JObjectFields<K, V, F> copy();
}