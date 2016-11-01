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

import com.jivesoftware.os.upena.shared.ChaosState;
import com.jivesoftware.os.upena.shared.InstanceKey;
import com.jivesoftware.os.upena.shared.ServiceKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ChaosStateGenerator {

    public enum PartitionStrategy {
        NONE("No network partition; all routes open"),
        ISOLATED("Isolate each node"),
        HALVE("Split partition in random halves"),
        MITM("Split partition in random halves with a node in the middle"),
        CIRCULAR("Communication flows in set circular direction"),
        RANDOM_CIRCULAR("Communication flows in random circular direction");

        public final String description;

        PartitionStrategy(String description) {
            this.description = description;
        }
    }

    private static InstanceKey popRandom(Set<InstanceKey> instances) {
        Iterator<InstanceKey> iter = instances.iterator();
        for (int i = 0; i < new Random().nextInt(instances.size()); i++) {
            iter.next();
        }

        InstanceKey res = iter.next();
        instances.remove(res);

        return res;
    }

    private static Map<InstanceKey, Set<InstanceKey>> genNone(Set<InstanceKey> instances) {
        Map<InstanceKey, Set<InstanceKey>> res = new HashMap<>();

        for (InstanceKey ik : instances) {
            res.put(ik, instances);
        }

        return res;
    }

    private static Map<InstanceKey, Set<InstanceKey>> genSelf(Set<InstanceKey> instances) {
        Map<InstanceKey, Set<InstanceKey>> res = new HashMap<>();

        for (InstanceKey ik : instances) {
            res.put(ik, new HashSet<>(Collections.singletonList(ik)));
        }

        return res;
    }

    private static Map<InstanceKey, Set<InstanceKey>> genHalve(Set<InstanceKey> instances) {
        Map<InstanceKey, Set<InstanceKey>> res = genSelf(instances);

        Set<InstanceKey> instancesCopy = new HashSet<>(instances);
        Set<InstanceKey> instancesRandomHalf = new HashSet<>();

        for (int i = 0; i < instances.size() / 2; i++) {
            instancesRandomHalf.add(popRandom(instancesCopy));
        }

        for (InstanceKey ik : instancesRandomHalf) {
            res.put(ik, instancesRandomHalf);
        }

        for (InstanceKey ik : instancesCopy) {
            res.put(ik, instancesCopy);
        }

        return res;
    }

    private static Map<InstanceKey, Set<InstanceKey>> genMitm(Set<InstanceKey> instances) {
        Map<InstanceKey, Set<InstanceKey>> res = genSelf(instances);

        Set<InstanceKey> instancesCopy = new HashSet<>(instances);
        InstanceKey mitm = popRandom(instancesCopy);
        //System.out.println("mitm: " + mitm);

        Set<InstanceKey> instancesCopyCopy = new HashSet<>(instancesCopy);

        Set<InstanceKey> instancesRandomHalf = new HashSet<>();
        for (int i = 0; i < instancesCopy.size() / 2; i++) {
            instancesRandomHalf.add(popRandom(instancesCopyCopy));
        }

        for (InstanceKey ik : instancesRandomHalf) {
            Set<InstanceKey> iks = new HashSet<>(instancesRandomHalf);
            iks.add(mitm);
            res.put(ik, iks);
        }

        for (InstanceKey ik : instancesCopyCopy) {
            Set<InstanceKey> iks = new HashSet<>(instancesCopyCopy);
            iks.add(mitm);
            res.put(ik, iks);
        }

        res.put(mitm, instances);

        return res;
    }

    private static Map<InstanceKey, Set<InstanceKey>> genRandomCircular(Set<InstanceKey> instances) {
        Map<InstanceKey, Set<InstanceKey>> res = genSelf(instances);

        Set<InstanceKey> instancesCopy = new HashSet<>(instances);
        InstanceKey first = popRandom(instancesCopy);

        InstanceKey cur, prev;
        cur = prev = first;

        while (instancesCopy.size() > 0) {
            cur = popRandom(instancesCopy);

            res.get(prev).add(cur);
            prev = cur;
        }

        if (cur != first) {
            res.get(cur).add(first);
        }

        return res;
    }

    private static Map<InstanceKey, Set<InstanceKey>> genCircular(Set<InstanceKey> instances) {
        Map<InstanceKey, Set<InstanceKey>> res = genSelf(instances);

        Iterator<InstanceKey> iter = instances.iterator();
        InstanceKey first = iter.next();

        InstanceKey cur, prev;
        cur = prev = first;

        while (iter.hasNext()) {
            cur = iter.next();

            res.get(prev).add(cur);
            prev = cur;
        }

        if (cur != first) {
            res.get(cur).add(first);
        }

        return res;
    }

    public static ChaosState create(ServiceKey serviceKey,
                                    Set<InstanceKey> instances,
                                    Map<String, String> properties) {
        Map<InstanceKey, Set<InstanceKey>> routes;

        PartitionStrategy partitionStrategy = ChaosStateHelper.parsePartitionType(properties);
        long interval = ChaosStateHelper.parseInterval(properties);

        switch (partitionStrategy) {
            case NONE:
                routes = genNone(instances);
                break;
            case ISOLATED:
                routes = genSelf(instances);
                break;
            case HALVE:
                routes = genHalve(instances);
                break;
            case MITM:
                routes = genMitm(instances);
                break;
            case CIRCULAR:
                routes = genCircular(instances);
                break;
            case RANDOM_CIRCULAR:
                routes = genRandomCircular(instances);
                break;
            default:
                throw new IllegalArgumentException("Unknown partition strategy " + partitionStrategy.toString());
        }

        return new ChaosState(serviceKey, System.currentTimeMillis() + interval / 2, 0, routes, properties);
    }

}
