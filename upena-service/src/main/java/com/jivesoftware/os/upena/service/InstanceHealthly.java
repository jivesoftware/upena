package com.jivesoftware.os.upena.service;

import com.jivesoftware.os.upena.shared.InstanceKey;

/**
 * Created by jonathan.colt on 1/9/17.
 */
public interface InstanceHealthly {
    boolean isHealth(InstanceKey key, String version) throws Exception;
}
