package com.jivesoftware.os.upena.ui;

import com.jivesoftware.os.upena.shared.Key;

public class FilterUtils {

    static <K extends Key> K nullIfEmpty(K key) {
        if (key.getKey().length() == 0) {
            return null;
        }
        return key;
    }
}
