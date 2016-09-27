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

import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.service.ChaosStateGenerator.PartitionStrategy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChaosStateHelper {

    public static final String IPV4_DEV_NULL = "240.0.0.0";
    public static final String PARTITION_TYPE = "partition_type";
    public static final String PARTITION_INTERVAL = "partition_interval";

    static boolean instancesMatch(Map<InstanceKey, Set<InstanceKey>> instanceRoutes,
                                  Set<InstanceKey> instances) {
        if (instanceRoutes == null) return false;
        if (instances == null) return false;
        if (instanceRoutes.size() != instances.size()) return false;

        Set<InstanceKey> drainInstances = new HashSet<>(instances);
        for (Map.Entry<InstanceKey, Set<InstanceKey>> entry : instanceRoutes.entrySet()) {
            drainInstances.remove(entry.getKey());
        }

        return drainInstances.isEmpty();
    }

    static boolean propertiesMatch(Map<String, String> chaosProperties,
                                   Map<String, String> chaosStateProperties) {
        if (chaosProperties == null) return false;
        if (chaosStateProperties == null) return false;
        if (chaosProperties.size() != chaosStateProperties.size()) return false;

        Map<String, String> drainProperties = new HashMap<>(chaosStateProperties);
        for (Map.Entry<String, String> entry : chaosProperties.entrySet()) {
            drainProperties.remove(entry.getKey());
        }

        return drainProperties.isEmpty();
    }

    static ChaosStateGenerator.PartitionStrategy parsePartitionType(Map<String, String> properties) {
        PartitionStrategy res = PartitionStrategy.NONE;

        for (Map.Entry<String, String> property : properties.entrySet()) {
            String propertyName = property.getKey();
            String propertyValue = property.getValue();

            if (propertyName.equals(ChaosStateHelper.PARTITION_TYPE)) {
                try {
                    res = ChaosStateGenerator.PartitionStrategy.valueOf(propertyValue);
                    break;
                } catch (IllegalArgumentException e) {
                    // ignore
                }
            }
        }

        return res;
    }

    static long parseInterval(Map<String, String> properties) {
        long res = 600_000L;

        for (Map.Entry<String, String> property : properties.entrySet()) {
            String propertyName = property.getKey();
            String propertyValue = property.getValue();

            if (propertyName.equals(ChaosStateHelper.PARTITION_INTERVAL)) {
                try {
                    res = Long.valueOf(propertyValue);
                    break;
                } catch (IllegalArgumentException e) {
                    // ignore
                }
            }
        }

        return res;
    }

}
