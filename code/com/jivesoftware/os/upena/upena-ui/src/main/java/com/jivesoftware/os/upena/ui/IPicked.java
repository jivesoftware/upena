package com.jivesoftware.os.upena.ui;

import com.jivesoftware.os.upena.shared.Stored;

public interface IPicked<K, V extends Stored> {

    void picked(K key, V v);
}
